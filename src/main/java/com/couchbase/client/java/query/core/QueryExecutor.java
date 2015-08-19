/**
 * Copyright (C) 2015 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */
package com.couchbase.client.java.query.core;

import com.couchbase.client.core.BackpressureException;
import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.RequestCancelledException;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.message.query.GenericQueryRequest;
import com.couchbase.client.core.message.query.GenericQueryResponse;
import com.couchbase.client.core.utils.Buffers;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.QueryExecutionException;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.query.AsyncQueryResult;
import com.couchbase.client.java.query.AsyncQueryRow;
import com.couchbase.client.java.query.DefaultAsyncQueryResult;
import com.couchbase.client.java.query.DefaultAsyncQueryRow;
import com.couchbase.client.java.query.ParameterizedQuery;
import com.couchbase.client.java.query.PrepareStatement;
import com.couchbase.client.java.query.PreparedPayload;
import com.couchbase.client.java.query.PreparedQuery;
import com.couchbase.client.java.query.Query;
import com.couchbase.client.java.query.QueryMetrics;
import com.couchbase.client.java.query.SimpleQuery;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.util.LRUCache;
import rx.Observable;
import rx.exceptions.CompositeException;
import rx.functions.Action0;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static com.couchbase.client.java.CouchbaseAsyncBucket.JSON_OBJECT_TRANSCODER;

/**
 * A class used to execute various N1QL queries.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Private
public class QueryExecutor {

    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(QueryExecutor.class);

    /**
     * The maximum number of cached queries after which the eldest will be evicted.
     */
    private static final int QUERY_CACHE_SIZE = 5000;

    private static final String ERROR_FIELD_CODE = "code";
    private static final String ERROR_FIELD_MSG = "msg";
    protected static final String ERROR_5000_SPECIFIC_MESSAGE = "index deleted or node hosting the index is down " +
            "- cause: queryport.indexNotFound";

    /**
     * Tests a N1QL error JSON for conditions warranting a prepared statement retry.
     */
    private static boolean shouldRetry(JsonObject errorJson) {
        if (errorJson == null) return false;
        Integer code = errorJson.getInt(ERROR_FIELD_CODE);
        String msg = errorJson.getString(ERROR_FIELD_MSG);

        if (code == null || msg == null) return false;

        if (code == 4050 || code == 4070 ||
                (code == 5000 && msg.contains(ERROR_5000_SPECIFIC_MESSAGE))) {
            return true;
        }
        return false;
    }

    /**
     * Peeks into the error stream of the {@link AsyncQueryResult} and reemit a copy of it if no retry condition for
     * prepared statement execution is found, otherwise emit an error than will trigger a retry (see {@link #shouldRetry(JsonObject)}).
     */
    private static final Func1<AsyncQueryResult, Observable<AsyncQueryResult>> QUERY_RESULT_PEEK_FOR_RETRY =
    new Func1<AsyncQueryResult, Observable<AsyncQueryResult>>() {
        @Override
        public Observable<AsyncQueryResult> call(final AsyncQueryResult aqr) {
            final Observable<JsonObject> cachedErrors = aqr.errors().cache();

            return cachedErrors
                    //only keep errors that triggers a prepared statement retry
                    .filter(new Func1<JsonObject, Boolean>() {
                        @Override
                        public Boolean call(JsonObject e) {
                            return shouldRetry(e);
                        }
                    })
                    //if none, will emit null
                    .lastOrDefault(null)
                    //... in which case a copy of the AsyncQueryResult is propagated, otherwise an retry
                    // triggering exception is propagated.
                    .flatMap(new Func1<JsonObject, Observable<AsyncQueryResult>>() {
                        @Override
                        public Observable<AsyncQueryResult> call(JsonObject errorJson) {
                            if (errorJson == null) {
                                AsyncQueryResult copyResult = new DefaultAsyncQueryResult(
                                        aqr.rows(), aqr.signature(), aqr.info(),
                                        cachedErrors,
                                        aqr.finalSuccess(), aqr.parseSuccess(), aqr.requestId(),
                                        aqr.clientContextId());
                                return Observable.just(copyResult);
                            } else {
                                return Observable.error(new QueryExecutionException("Error with prepared query",
                                        errorJson));
                            }
                        }
                    });
        }
    };

    private final ClusterFacade core;
    private final String bucket;
    private final String password;
    private final Map<String, PreparedPayload> queryCache;

    /**
     * Construct a new QueryExecutor that will send requests through the given {@link ClusterFacade}. For queries that
     * are not ad-hoc, it will cache up to {@value #QUERY_CACHE_SIZE} queries.
     *
     * @param core the core through which to send requests.
     * @param bucket the bucket to bootstrap from.
     * @param password the password for the bucket.
     */
    public QueryExecutor(ClusterFacade core, String bucket, String password) {
        this(core, bucket, password, new LRUCache<String, PreparedPayload>(QUERY_CACHE_SIZE));
    }

    /**
     * This constructor is for testing purpose, prefer using {@link #QueryExecutor(ClusterFacade, String, String)}.
     */
    protected QueryExecutor(ClusterFacade core, String bucket, String password, LRUCache<String, PreparedPayload> lruCache) {
        this.core = core;
        this.bucket = bucket;
        this.password = password;

        queryCache = Collections.synchronizedMap(lruCache);
    }

    public Observable<AsyncQueryResult> execute(final Query query) {
        if (query.params().isAdhoc()) {
            return executeQuery(query);
        } else {
            return dispatchPrepared(query);
        }
    }

    protected Observable<AsyncQueryResult> dispatchPrepared(final Query query) {
        PreparedPayload payload = queryCache.get(query.statement().toString());
        Func1<Throwable, Observable<AsyncQueryResult>> retryFunction = new Func1<Throwable, Observable<AsyncQueryResult>>() {
            @Override
            public Observable<AsyncQueryResult> call(Throwable throwable) {
                return retryPrepareAndExecuteOnce(throwable, query);
            }
        };

        if (payload != null) {
            //EXECUTE, if relevant error PREPARE + EXECUTE
            return executePrepared(query, payload)
                    .flatMap(QUERY_RESULT_PEEK_FOR_RETRY)
                    .onErrorResumeNext(retryFunction);
        } else {
            //PREPARE, EXECUTE, if relevant error, PREPARE again + EXECUTE
            return prepareAndExecute(query)
                .flatMap(QUERY_RESULT_PEEK_FOR_RETRY)
                .onErrorResumeNext(retryFunction);
        }
    }

    /**
     * In case the error warrants a retry, issue a PREPARE, followed by an update
     * of the cache and an EXECUTE.
     * Any failure in the EXECUTE won't continue the retry cycle.
     */
    protected Observable<AsyncQueryResult> retryPrepareAndExecuteOnce(Throwable error, Query query) {
        if (error instanceof QueryExecutionException &&
                shouldRetry(((QueryExecutionException) error).getN1qlError())) {
            return prepareAndExecute(query);
        }
        return Observable.error(error);
    }

    /**
     * Issues a N1QL PREPARE, puts the plan in cache then EXECUTE it.
     */
    protected Observable<AsyncQueryResult> prepareAndExecute(final Query query) {
        return prepare(query.statement())
                .flatMap(new Func1<PreparedPayload, Observable<AsyncQueryResult>>() {
                    @Override
                    public Observable<AsyncQueryResult> call(PreparedPayload payload) {
                        queryCache.put(query.statement().toString(), payload);
                        return executePrepared(query, payload);
                    }
                });
    }

    /**
     * Issues a proper N1QL EXECUTE, detecting if parameters must be added to it.
     */
    protected Observable<AsyncQueryResult> executePrepared(final Query query, PreparedPayload payload) {
        if (query instanceof ParameterizedQuery) {
            ParameterizedQuery pq = (ParameterizedQuery) query;
            if (pq.isPositional()) {
                return executeQuery(
                    new PreparedQuery(payload, (JsonArray) pq.statementParameters(), query.params())
                );
            } else {
                return executeQuery(
                    new PreparedQuery(payload, (JsonObject) pq.statementParameters(), query.params())
                );
            }
        } else {
            return executeQuery(new PreparedQuery(payload, query.params()));
        }
    }

    /**
     *
     * Experimental, Internal: Queries a N1QL secondary index.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while "in flight" on the wire: {@link RequestCancelledException}
     *
     * @param query the full query as a Json String, including all necessary parameters.
     * @return a result containing all found rows and additional information.
     */
    protected Observable<AsyncQueryResult> executeQuery(final Query query) {
        return Observable.defer(new Func0<Observable<GenericQueryResponse>>() {
            @Override
            public Observable<GenericQueryResponse> call() {
                return core.send(GenericQueryRequest.jsonQuery(query.n1ql().toString(), bucket, password));
            }
        }).flatMap(new Func1<GenericQueryResponse, Observable<AsyncQueryResult>>() {
            @Override
            public Observable<AsyncQueryResult> call(final GenericQueryResponse response) {
                final Observable<AsyncQueryRow> rows = response.rows().map(new Func1<ByteBuf, AsyncQueryRow>() {
                    @Override
                    public AsyncQueryRow call(ByteBuf byteBuf) {
                        try {
                            JsonObject value = JSON_OBJECT_TRANSCODER.byteBufToJsonObject(byteBuf);
                            return new DefaultAsyncQueryRow(value);
                        } catch (Exception e) {
                            throw new TranscodingException("Could not decode N1QL Query Row.", e);
                        } finally {
                            byteBuf.release();
                        }
                    }
                });
                final Observable<Object> signature = response.signature().map(new Func1<ByteBuf, Object>() {
                    @Override
                    public Object call(ByteBuf byteBuf) {
                        try {
                            return JSON_OBJECT_TRANSCODER.byteBufJsonValueToObject(byteBuf);
                        } catch (Exception e) {
                            throw new TranscodingException("Could not decode N1QL Query Signature", e);
                        } finally {
                            byteBuf.release();
                        }
                    }
                });
                final Observable<QueryMetrics> info = response.info().map(new Func1<ByteBuf, JsonObject>() {
                    @Override
                    public JsonObject call(ByteBuf byteBuf) {
                        try {
                            return JSON_OBJECT_TRANSCODER.byteBufToJsonObject(byteBuf);
                        } catch (Exception e) {
                            throw new TranscodingException("Could not decode N1QL Query Info.", e);
                        } finally {
                            byteBuf.release();
                        }
                    }
                })
                .map(new Func1<JsonObject, QueryMetrics>() {
                    @Override
                    public QueryMetrics call(JsonObject jsonObject) {
                        return new QueryMetrics(jsonObject);
                    }
                });
                final Observable<Boolean> finalSuccess = response.queryStatus().map(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        return "success".equalsIgnoreCase(s) || "completed".equalsIgnoreCase(s);
                    }
                });
                final Observable<JsonObject> errors = response.errors().map(new Func1<ByteBuf, JsonObject>() {
                    @Override
                    public JsonObject call(ByteBuf byteBuf) {
                        try {
                            return JSON_OBJECT_TRANSCODER.byteBufToJsonObject(byteBuf);
                        } catch (Exception e) {
                            throw new TranscodingException("Could not decode View Info.", e);
                        } finally {
                            byteBuf.release();
                        }
                    }
                });
                boolean parseSuccess = response.status().isSuccess();
                String contextId = response.clientRequestId() == null ? "" : response.clientRequestId();
                String requestId = response.requestId();

                AsyncQueryResult r = new DefaultAsyncQueryResult(rows, signature, info, errors,
                        finalSuccess, parseSuccess, requestId, contextId);
                return Observable.just(r);
            }
        });
    }

    /**
     * Experimental: Queries a N1QL secondary index and prepare an execution plan via the given
     * statement in {@link String} form. Statement can contain placeholders.
     * The resulting {@link PreparedPayload} can be cached and (re)used later in a {@link PreparedQuery}.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     *
     * @param statement the statement to prepare a plan for.
     * @return a {@link PreparedPayload} that can be cached and reused later in {@link PreparedQuery}.
     */
    protected Observable<PreparedPayload> prepare(Statement statement) {
        final PrepareStatement prepared;
        if (statement instanceof PrepareStatement) {
            prepared = (PrepareStatement) statement;
        } else {
            prepared = PrepareStatement.prepare(statement, null);
        }
        final SimpleQuery query = Query.simple(prepared);

        return Observable.defer(new Func0<Observable<GenericQueryResponse>>() {
            @Override
            public Observable<GenericQueryResponse> call() {
                return core.send(GenericQueryRequest.jsonQuery(query.n1ql().toString(), bucket, password));
            }
        }).flatMap(new Func1<GenericQueryResponse, Observable<PreparedPayload>>() {
            @Override
            public Observable<PreparedPayload> call(GenericQueryResponse r) {
                if (r.status().isSuccess()) {
                    r.info().subscribe(Buffers.BYTE_BUF_RELEASER);
                    r.signature().subscribe(Buffers.BYTE_BUF_RELEASER);
                    r.errors().subscribe(Buffers.BYTE_BUF_RELEASER);
                    return r.rows().map(new Func1<ByteBuf, PreparedPayload>() {
                        @Override
                        public PreparedPayload call(ByteBuf byteBuf) {
                            try {
                                JsonObject value = JSON_OBJECT_TRANSCODER.byteBufToJsonObject(byteBuf);
                                return new PreparedPayload(
                                    prepared.originalStatement(),
                                    value.getString("name"),
                                    value.getString("encoded_plan")
                                );
                            } catch (Exception e) {
                                throw new TranscodingException("Could not decode N1QL Query Plan.", e);
                            } finally {
                                byteBuf.release();
                            }
                        }
                    });
                } else {
                    r.info().subscribe(Buffers.BYTE_BUF_RELEASER);
                    r.signature().subscribe(Buffers.BYTE_BUF_RELEASER);
                    r.rows().subscribe(Buffers.BYTE_BUF_RELEASER);
                    return r.errors().map(new Func1<ByteBuf, Exception>() {
                        @Override
                        public Exception call(ByteBuf byteBuf) {
                            try {
                                JsonObject value = JSON_OBJECT_TRANSCODER.byteBufToJsonObject(byteBuf);
                                return new CouchbaseException("Query Error - " + value.toString());
                            } catch (Exception e) {
                                throw new TranscodingException("Could not decode N1QL Query Plan.", e);
                            } finally {
                                byteBuf.release();
                            }
                        }
                    })
                        .reduce(new ArrayList<Throwable>(),
                            new Func2<ArrayList<Throwable>, Exception, ArrayList<Throwable>>() {
                                @Override
                                public ArrayList<Throwable> call(ArrayList<Throwable> throwables,
                                                                 Exception error) {
                                    throwables.add(error);
                                    return throwables;
                                }
                            })
                        .flatMap(new Func1<ArrayList<Throwable>, Observable<PreparedPayload>>() {
                            @Override
                            public Observable<PreparedPayload> call(ArrayList<Throwable> errors) {
                                if (errors.size() == 1) {
                                    return Observable.error(new CouchbaseException(
                                        "Error while preparing plan", errors.get(0)));
                                } else {
                                    return Observable.error(new CompositeException(
                                        "Multiple errors while preparing plan", errors));
                                }
                            }
                        });
                }
            }
        });
    }

    /**
     * Invalidates and clears the query cache.
     */
    public int invalidateQueryCache() {
        int oldSize = queryCache.size();
        queryCache.clear();
        return oldSize;
    }

}
