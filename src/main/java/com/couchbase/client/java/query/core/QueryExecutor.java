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
import com.couchbase.client.core.retry.RetryStrategy;
import com.couchbase.client.core.utils.Buffers;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.query.AsyncQueryResult;
import com.couchbase.client.java.query.AsyncQueryRow;
import com.couchbase.client.java.query.DefaultAsyncQueryResult;
import com.couchbase.client.java.query.DefaultAsyncQueryRow;
import com.couchbase.client.java.query.NamedPreparedStatementException;
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

    private final ClusterFacade core;
    private final String bucket;
    private final String password;
    private final CouchbaseEnvironment environment;
    private final Map<String, PreparedPayload> queryCache;

    public QueryExecutor(ClusterFacade core, String bucket, String password, CouchbaseEnvironment environment) {
        this.core = core;
        this.bucket = bucket;
        this.password = password;
        this.environment = environment;

        queryCache = Collections.synchronizedMap(new LRUCache<String, PreparedPayload>(QUERY_CACHE_SIZE));
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
    public Observable<AsyncQueryResult> executeRaw(final String query) {
        return Observable.defer(new Func0<Observable<GenericQueryResponse>>() {
            @Override
            public Observable<GenericQueryResponse> call() {
                return core.send(GenericQueryRequest.jsonQuery(query, bucket, password));
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
    public Observable<PreparedPayload> prepare(Statement statement) {
        final PrepareStatement prepared;
        if (statement instanceof PrepareStatement) {
            prepared = (PrepareStatement) statement;
        } else {
            prepared = PrepareStatement.prepare(statement);
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
                                String serverName = value.getString("name");
                                if (prepared.preparedName() != null && !prepared.preparedName().equals(serverName)) {
                                    throw new IllegalStateException("Prepared statement name from server differs: " +
                                            serverName + ", expected " + prepared.preparedName());
                                }
                                return new PreparedPayload(prepared.originalStatement(), prepared.preparedName());
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
     * Experimental, Internal: Execute a prepared query, without retry behavior.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The prepared statement's name was not known on the server and it was re-prepared:
     * {@link NamedPreparedStatementException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while "in flight" on the wire: {@link RequestCancelledException}
     *
     * @param query the prepared query (including parameters).
     * @return a result similar to {@link #executeRaw(String)} if the prepared query could be executed,
     * or similar to {@link #prepare(Statement)} )} if it had to be re-prepared on the node.
     */
    public Observable<AsyncQueryResult> executePrepared(final PreparedQuery query) {
        return executeRaw(query.n1ql().toString())
                .flatMap(new Func1<AsyncQueryResult, Observable<AsyncQueryResult>>() {
                    @Override
                    public Observable<AsyncQueryResult> call(final AsyncQueryResult aqr) {
                        final Observable<AsyncQueryRow> cachedRows = aqr.rows().cache();
                        return cachedRows
                                .firstOrDefault(null)
                                .flatMap(new Func1<AsyncQueryRow, Observable<AsyncQueryResult>>() {
                                    @Override
                                    public Observable<AsyncQueryResult> call(AsyncQueryRow row) {
                                        if (row != null && row.value().containsKey("operator")
                                                && row.value().getObject("operator").containsKey("#operator")) {
                                            return Observable.error(
                                                    new NamedPreparedStatementException("Named prepared statement "
                                                            + query.statement().preparedName()
                                                            + " not found, it has been re-prepared"));
                                        }

                                        AsyncQueryResult copyResult = new DefaultAsyncQueryResult(cachedRows,
                                                aqr.signature(), aqr.info(), aqr.errors(), aqr.finalSuccess(),
                                                aqr.parseSuccess(), aqr.requestId(), aqr.clientContextId());

                                        return Observable.just(copyResult);
                                    }
                                });
                    }
                });
    }

    /**
     * Experimental, Internal: Execute a prepared query, with retry behavior (at least one retry, maybe more depending
     * on the {@link RetryStrategy}) instead of throwing a {@link NamedPreparedStatementException} immediately.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The prepared statement's name was not known on the server and it was re-prepared:
     * {@link NamedPreparedStatementException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while "in flight" on the wire: {@link RequestCancelledException}
     *
     * @param query the prepared query (including parameters).
     * @return a result similar to {@link #executeRaw(String)} if the prepared query could be executed,
     * or similar to {@link #prepare(Statement)} )} if it had to be re-prepared on the node.
     */
    public Observable<AsyncQueryResult> executePreparedWithRetry(final PreparedQuery query) {
        //for the purpose of deciding to retry or not
        final GenericQueryRequest request = GenericQueryRequest.jsonQuery(query.n1ql().toString(), bucket, password);

        return executePrepared(query)
                .retry(new Func2<Integer, Throwable, Boolean>() {
                    @Override
                    public Boolean call(Integer attempts, Throwable throwable) {
                        if (throwable instanceof NamedPreparedStatementException
                                && (attempts == 1 || environment.retryStrategy().shouldRetry(request, environment))) {
                            LOGGER.warn("Retrying #" + attempts + " prepared query " + query.n1ql());
                            return true;
                        }
                        return false;
                    }
                });
    }
}
