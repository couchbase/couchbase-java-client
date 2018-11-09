/*
 * Copyright (c) 2016 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.client.java.query.core;

import com.couchbase.client.core.BackpressureException;
import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.RequestCancelledException;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.config.NodeInfo;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.message.CouchbaseResponse;
import com.couchbase.client.core.message.cluster.GetClusterConfigRequest;
import com.couchbase.client.core.message.cluster.GetClusterConfigResponse;
import com.couchbase.client.core.message.query.GenericQueryRequest;
import com.couchbase.client.core.message.query.GenericQueryResponse;
import com.couchbase.client.core.service.ServiceType;
import com.couchbase.client.core.utils.Buffers;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.CouchbaseAsyncBucket;
import com.couchbase.client.java.bucket.api.Utils;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.error.QueryExecutionException;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.query.AsyncN1qlQueryResult;
import com.couchbase.client.java.query.AsyncN1qlQueryRow;
import com.couchbase.client.java.query.DefaultAsyncN1qlQueryResult;
import com.couchbase.client.java.query.DefaultAsyncN1qlQueryRow;
import com.couchbase.client.java.query.DefaultN1qlQueryResult;
import com.couchbase.client.java.query.N1qlMetrics;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.ParameterizedN1qlQuery;
import com.couchbase.client.java.query.PrepareStatement;
import com.couchbase.client.java.query.PreparedN1qlQuery;
import com.couchbase.client.java.query.PreparedPayload;
import com.couchbase.client.java.query.SimpleN1qlQuery;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.transcoder.TranscoderUtils;
import com.couchbase.client.java.util.LRUCache;
import io.opentracing.tag.Tags;
import rx.Observable;
import rx.Subscriber;
import rx.exceptions.CompositeException;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func7;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.couchbase.client.java.CouchbaseAsyncBucket.JSON_OBJECT_TRANSCODER;
import static com.couchbase.client.java.bucket.api.Utils.applyTimeout;
import static com.couchbase.client.java.util.OnSubscribeDeferAndWatch.deferAndWatch;

/**
 * A class used to execute various N1QL queries.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Private
public class N1qlQueryExecutor {

    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(N1qlQueryExecutor.class);

    /**
     * The maximum number of cached queries after which the eldest will be evicted.
     */
    private static final int QUERY_CACHE_SIZE = 5000;

    private static final String ERROR_FIELD_CODE = "code";
    private static final String ERROR_FIELD_MSG = "msg";
    protected static final String ERROR_5000_SPECIFIC_MESSAGE = "queryport.indexNotFound";

    public static final java.lang.String ENCODED_PLAN_ENABLED_PROPERTY = "com.couchbase.query.encodedPlanEnabled";

    private final ClusterFacade core;
    private final String bucket;
    private final String username;
    private final String password;
    private final Map<String, PreparedPayload> queryCache;
    private final boolean encodedPlanEnabled;

    /**
     * Construct a new N1qlQueryExecutor that will send requests through the given {@link ClusterFacade}. For queries that
     * are not ad-hoc, it will cache up to {@link #QUERY_CACHE_SIZE} queries.
     *
     * @param core the core through which to send requests.
     * @param bucket the bucket to bootstrap from.
     * @param username the user authorized for bucket access.
     * @param password the password for the bucket.
     */
    public N1qlQueryExecutor(ClusterFacade core, String bucket, String username, String password) {
        this(core, bucket, username, password, new LRUCache<String, PreparedPayload>(QUERY_CACHE_SIZE), true);
    }

    /**
     * Construct a new N1qlQueryExecutor that will send requests through the given {@link ClusterFacade}. For queries that
     * are not ad-hoc, it will cache up to {@link #QUERY_CACHE_SIZE} queries.
     *
     * @param core the core through which to send requests.
     * @param bucket the bucket to bootstrap from.
     * @param password the password for the bucket.
     */
    public N1qlQueryExecutor(ClusterFacade core, String bucket, String password) {
        this(core, bucket, bucket, password, new LRUCache<String, PreparedPayload>(QUERY_CACHE_SIZE), true);
    }

    /**
     * Construct a new N1qlQueryExecutor that will send requests through the given {@link ClusterFacade}. For queries that
     * are not ad-hoc, it will cache up to {@link #QUERY_CACHE_SIZE} queries.
     *
     * @param core the core through which to send requests.
     * @param bucket the bucket to bootstrap from.
     * @param password the password for the bucket.
     * @param encodedPlanEnabled true to include an encoded plan when running prepared queries, false otherwise.
     */
    public N1qlQueryExecutor(ClusterFacade core, String bucket, String password, boolean encodedPlanEnabled) {
        this(core, bucket, bucket, password, new LRUCache<String, PreparedPayload>(QUERY_CACHE_SIZE), encodedPlanEnabled);
    }

    /**
     * Construct a new N1qlQueryExecutor that will send requests through the given {@link ClusterFacade}. For queries that
     * are not ad-hoc, it will cache up to {@link #QUERY_CACHE_SIZE} queries.
     *
     * @param core the core through which to send requests.
     * @param bucket the bucket to bootstrap from.
     * @param username the user authorized for bucket access.
     * @param password the password for the user.
     * @param encodedPlanEnabled true to include an encoded plan when running prepared queries, false otherwise.
     */
    public N1qlQueryExecutor(ClusterFacade core, String bucket, String username, String password, boolean encodedPlanEnabled) {
        this(core, bucket, username, password, new LRUCache<String, PreparedPayload>(QUERY_CACHE_SIZE), encodedPlanEnabled);
    }

    /**
     * This constructor is for testing purpose, prefer using {@link #N1qlQueryExecutor(ClusterFacade, String, String, String)}.
     */
    protected N1qlQueryExecutor(ClusterFacade core, String bucket, String username, String password,
            LRUCache<String, PreparedPayload> lruCache, boolean encodedPlanEnabled) {
        this.core = core;
        this.bucket = bucket;
        this.username = username;
        this.password = password;
        this.encodedPlanEnabled = encodedPlanEnabled;

        queryCache = Collections.synchronizedMap(lruCache);
    }

    public Observable<AsyncN1qlQueryResult> execute(final N1qlQuery query, CouchbaseEnvironment env, long timeout, TimeUnit timeUnit) {
        if (query.params().isAdhoc()) {
            return executeQuery(query, env, timeout, timeUnit);
        } else {
            return dispatchPrepared(query, env, timeout, timeUnit);
        }
    }

    /**
     *
     * Internal: Queries a N1QL secondary index.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while "in flight" on the wire: {@link RequestCancelledException}
     *
     * @param query the full query as a Json String, including all necessary parameters.
     * @return a result containing all found rows and additional information.
     */
    protected Observable<AsyncN1qlQueryResult> executeQuery(final N1qlQuery query,
                                                            final CouchbaseEnvironment env, final long timeout, final TimeUnit timeUnit) {
        return deferAndWatch(new Func1<Subscriber, Observable<GenericQueryResponse>>() {
            @Override
            public Observable<GenericQueryResponse> call(Subscriber subscriber) {
                GenericQueryRequest request = createN1qlRequest(query, bucket, username, password, null);
                Utils.addRequestSpan(env, request, "n1ql");
                if (env.operationTracingEnabled()) {
                    request.span().setTag(Tags.DB_STATEMENT.getKey(), query.statement().toString());
                }
                request.subscriber(subscriber);
                return applyTimeout(core.<GenericQueryResponse>send(request), request, env, timeout, timeUnit);
            }
        }).flatMap(new Func1<GenericQueryResponse, Observable<AsyncN1qlQueryResult>>() {
            @Override
            public Observable<AsyncN1qlQueryResult> call(final GenericQueryResponse response) {
                final Observable<AsyncN1qlQueryRow> rows = response.rows().map(new Func1<ByteBuf, AsyncN1qlQueryRow>() {
                    @Override
                    public AsyncN1qlQueryRow call(ByteBuf byteBuf) {
                        try {
                            byte[] copy = TranscoderUtils.copyByteBufToByteArray(byteBuf);
                            return new DefaultAsyncN1qlQueryRow(copy);
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
                final Observable<N1qlMetrics> info = response.info().map(new Func1<ByteBuf, JsonObject>() {
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
                .map(new Func1<JsonObject, N1qlMetrics>() {
                    @Override
                    public N1qlMetrics call(JsonObject jsonObject) {
                        return new N1qlMetrics(jsonObject);
                    }
                });
                final Observable<String> finalStatus = response.queryStatus();
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

                final Observable<JsonObject> profileInfo = response.profileInfo().map(new Func1<ByteBuf, JsonObject>() {
                    @Override
                    public JsonObject call(ByteBuf byteBuf) {
                        try {
                            return JSON_OBJECT_TRANSCODER.byteBufToJsonObject(byteBuf);
                        } catch (Exception e) {
                            throw new TranscodingException("Could not decode profile Info.", e);
                        } finally {
                            byteBuf.release();
                        }
                    }
                });

                boolean parseSuccess = response.status().isSuccess();
                String contextId = response.clientRequestId() == null ? "" : response.clientRequestId();
                String requestId = response.requestId();

                AsyncN1qlQueryResult r = new DefaultAsyncN1qlQueryResult(rows, signature, info, errors, profileInfo,
                        finalStatus, parseSuccess, requestId, contextId);
                return Observable.just(r);
            }
        });
    }

    //==== Section related to prepared statements, PREPARE and EXECUTE support ====
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
     * Peeks into the error stream of the {@link AsyncN1qlQueryResult} and reemit a copy of it if no retry condition for
     * prepared statement execution is found, otherwise emit an error than will trigger a retry (see {@link #shouldRetry(JsonObject)}).
     */
    private static final Func1<AsyncN1qlQueryResult, Observable<AsyncN1qlQueryResult>> QUERY_RESULT_PEEK_FOR_RETRY =
    new Func1<AsyncN1qlQueryResult, Observable<AsyncN1qlQueryResult>>() {
        @Override
        public Observable<AsyncN1qlQueryResult> call(final AsyncN1qlQueryResult aqr) {
            if (!aqr.parseSuccess()) {
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
                        //... in which case a copy of the AsyncN1qlQueryResult is propagated, otherwise an retry
                        // triggering exception is propagated.
                        .flatMap(new Func1<JsonObject, Observable<AsyncN1qlQueryResult>>() {
                            @Override
                            public Observable<AsyncN1qlQueryResult> call(JsonObject errorJson) {
                                if (errorJson == null) {
                                    AsyncN1qlQueryResult copyResult = new DefaultAsyncN1qlQueryResult(
                                            aqr.rows(), aqr.signature(), aqr.info(),
                                            cachedErrors,
                                            aqr.profileInfo(),
                                            aqr.status(), aqr.parseSuccess(), aqr.requestId(),
                                            aqr.clientContextId());
                                    return Observable.just(copyResult);
                                } else {
                                    return Observable.error(new QueryExecutionException("Error with prepared query",
                                            errorJson));
                                }
                            }
                        });
            } else {
                return Observable.just(aqr);
            }
        }
    };

    protected Observable<AsyncN1qlQueryResult> dispatchPrepared(final N1qlQuery query, final CouchbaseEnvironment env, final long timeout, final TimeUnit timeUnit) {
        PreparedPayload payload = queryCache.get(query.statement().toString());
        Func1<Throwable, Observable<AsyncN1qlQueryResult>> retryFunction = new Func1<Throwable, Observable<AsyncN1qlQueryResult>>() {
            @Override
            public Observable<AsyncN1qlQueryResult> call(Throwable throwable) {
                return retryPrepareAndExecuteOnce(throwable, query, env, timeout, timeUnit);
            }
        };

        if (payload != null) {
            //EXECUTE, if relevant error PREPARE + EXECUTE
            return executePrepared(query, payload, env, timeout, timeUnit)
                    .flatMap(QUERY_RESULT_PEEK_FOR_RETRY)
                    .onErrorResumeNext(retryFunction);
        } else {
            //PREPARE, EXECUTE, if relevant error, PREPARE again + EXECUTE
            return prepareAndExecute(query, env, timeout, timeUnit)
                .flatMap(QUERY_RESULT_PEEK_FOR_RETRY)
                .onErrorResumeNext(retryFunction);
        }
    }

    /**
     * In case the error warrants a retry, issue a PREPARE, followed by an update
     * of the cache and an EXECUTE.
     * Any failure in the EXECUTE won't continue the retry cycle.
     */
    protected Observable<AsyncN1qlQueryResult> retryPrepareAndExecuteOnce(Throwable error, N1qlQuery query, CouchbaseEnvironment env, long timeout, TimeUnit timeUnit) {
        if (error instanceof QueryExecutionException &&
                shouldRetry(((QueryExecutionException) error).getN1qlError())) {
            queryCache.remove(query.statement().toString());
            return prepareAndExecute(query, env, timeout, timeUnit);
        }
        return Observable.error(error);
    }

    /**
     * Issues a N1QL PREPARE, puts the plan in cache then EXECUTE it.
     */
    protected Observable<AsyncN1qlQueryResult> prepareAndExecute(final N1qlQuery query, final CouchbaseEnvironment env, final long timeout, final TimeUnit timeUnit) {
        return prepare(query.statement())
                .flatMap(new Func1<PreparedPayload, Observable<AsyncN1qlQueryResult>>() {
                    @Override
                    public Observable<AsyncN1qlQueryResult> call(PreparedPayload payload) {
                        queryCache.put(query.statement().toString(), payload);
                        return executePrepared(query, payload, env, timeout, timeUnit);
                    }
                });
    }

    /**
     * Issues a proper N1QL EXECUTE, detecting if parameters must be added to it.
     */
    protected Observable<AsyncN1qlQueryResult> executePrepared(final N1qlQuery query, PreparedPayload payload, CouchbaseEnvironment env, long timeout, TimeUnit timeUnit) {
        PreparedN1qlQuery preparedQuery;
        if (query instanceof ParameterizedN1qlQuery) {
            ParameterizedN1qlQuery pq = (ParameterizedN1qlQuery) query;
            if (pq.isPositional()) {
                preparedQuery = new PreparedN1qlQuery(payload, (JsonArray) pq.statementParameters(), query.params());
            } else {
                preparedQuery = new PreparedN1qlQuery(payload, (JsonObject) pq.statementParameters(), query.params());
            }
        } else {
            preparedQuery = new PreparedN1qlQuery(payload, query.params());
        }
        preparedQuery.setEncodedPlanEnabled(isEncodedPlanEnabled());

        return executeQuery(preparedQuery, env, timeout, timeUnit);
    }

    /**
     * Queries a N1QL secondary index and prepare an execution plan via the given
     * statement in {@link String} form. Statement can contain placeholders.
     * The resulting {@link PreparedPayload} can be cached and (re)used later in a {@link PreparedN1qlQuery}.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     *
     * @param statement the statement to prepare a plan for.
     * @return a {@link PreparedPayload} that can be cached and reused later in {@link PreparedN1qlQuery}.
     */
    protected Observable<PreparedPayload> prepare(Statement statement) {
        final PrepareStatement prepared;
        if (statement instanceof PrepareStatement) {
            prepared = (PrepareStatement) statement;
        } else {
            //not including an explicit name here will produce a hash as explicit name
            //null would have let the server generate a name
            prepared = PrepareStatement.prepare(statement);
        }
        final SimpleN1qlQuery query = N1qlQuery.simple(prepared);

        Observable<GenericQueryResponse> source;


        if (isEncodedPlanEnabled()) {
            //we'll include the encodedPlan in each EXECUTE, so we don't broadcast during PREPARE
            source = deferAndWatch(new Func1<Subscriber, Observable<? extends GenericQueryResponse>>() {
                @Override
                public Observable<GenericQueryResponse> call(Subscriber subscriber) {
                    GenericQueryRequest request = createN1qlRequest(query, bucket, username, password, null);
                    request.subscriber(subscriber);
                    return core.send(request);
                }
            });
        } else {
            //we won't include the encoded plan in each EXECUTE, so we'll broadcast the PREPARE
            source = Observable.defer(new Func0<Observable<GetClusterConfigResponse>>() {
                @Override
                public Observable<GetClusterConfigResponse> call() {
                    return core.send(new GetClusterConfigRequest());
                }
            }).flatMap(new Func1<GetClusterConfigResponse, Observable<NodeInfo>>() {
                @Override
                public Observable<NodeInfo> call(GetClusterConfigResponse getClusterConfigResponse) {
                    return Observable.from(getClusterConfigResponse.config()
                            .bucketConfig(bucket)
                            .nodes());
                }
            }).filter(new Func1<NodeInfo, Boolean>() {
                @Override
                public Boolean call(NodeInfo nodeInfo) {
                    return nodeInfo.services().containsKey(ServiceType.QUERY)
                        || nodeInfo.sslServices().containsKey(ServiceType.QUERY);
                }
            }).flatMap(new Func1<NodeInfo, Observable<GenericQueryResponse>>() {
                @Override
                public Observable<GenericQueryResponse> call(NodeInfo nodeInfo) {
                    try {
                        InetAddress hostname = InetAddress.getByName(nodeInfo.hostname().address());
                        final GenericQueryRequest req = createN1qlRequest(query, bucket, username, password, hostname);
                        return deferAndWatch(new Func1<Subscriber, Observable<? extends GenericQueryResponse>>() {
                            @Override
                            public Observable<? extends GenericQueryResponse> call(Subscriber subscriber) {
                                req.subscriber(subscriber);
                                return core.send(req);
                            }
                        });
                    } catch (UnknownHostException e) {
                        return Observable.error(e);
                    }
                }
            });
        }

        return source.flatMap(new Func1<GenericQueryResponse, Observable<PreparedPayload>>() {
            @Override
            public Observable<PreparedPayload> call(GenericQueryResponse r) {
                if (r.status().isSuccess()) {
                    r.info().subscribe(Buffers.BYTE_BUF_RELEASER);
                    r.signature().subscribe(Buffers.BYTE_BUF_RELEASER);
                    r.errors().subscribe(Buffers.BYTE_BUF_RELEASER);
                    r.profileInfo().subscribe(Buffers.BYTE_BUF_RELEASER);
                    return r.rows().map(new Func1<ByteBuf, PreparedPayload>() {
                        @Override
                        public PreparedPayload call(ByteBuf byteBuf) {
                            try {
                                JsonObject value = JSON_OBJECT_TRANSCODER.byteBufToJsonObject(byteBuf);
                                return extractPreparedPayloadFromResponse(prepared, value);
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
                    r.profileInfo().subscribe(Buffers.BYTE_BUF_RELEASER);
                    return r.errors().map(new Func1<ByteBuf, Exception>() {
                        @Override
                        public Exception call(ByteBuf byteBuf) {
                            try {
                                JsonObject value = JSON_OBJECT_TRANSCODER.byteBufToJsonObject(byteBuf);
                                return new CouchbaseException("N1qlQuery Error - " + value.toString());
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
        }).last();
    }

    /**
     * Creates the core query request and performs centralized string substitution.
     */
    private GenericQueryRequest createN1qlRequest(final N1qlQuery query, String bucket, String username, String password,
            InetAddress targetNode) {
        String rawQuery = query.n1ql().toString();
        rawQuery = rawQuery.replaceAll(
          CouchbaseAsyncBucket.CURRENT_BUCKET_IDENTIFIER,
          "`" + bucket + "`"
        );
        if (targetNode != null) {
            return GenericQueryRequest.jsonQuery(rawQuery, bucket, username, password, targetNode, query.params().clientContextId());
        } else {
            return GenericQueryRequest.jsonQuery(rawQuery, bucket, username, password, query.params().clientContextId());
        }
    }

    /**
     * Extracts the {@link PreparedPayload} from the server's response during a PREPARE.
     */
    protected PreparedPayload extractPreparedPayloadFromResponse(PrepareStatement prepared, JsonObject response) {
        return new PreparedPayload(
                prepared.originalStatement(),
                response.getString("name"),
                response.getString("encoded_plan")
        );
    }

    /**
     * Invalidates and clears the query cache.
     */
    public int invalidateQueryCache() {
        int oldSize = queryCache.size();
        queryCache.clear();
        return oldSize;
    }

    /**
     * @return true if prepared queries produced by this QueryExecutor will include an encoded plan, false otherwise.
     */
    public boolean isEncodedPlanEnabled() {
        return this.encodedPlanEnabled;
    }

    /**
     * A function that can be used in a flatMap to convert an {@link AsyncN1qlQueryResult} to a {@link N1qlQueryResult}.
     */
    public static final Func1<? super AsyncN1qlQueryResult, ? extends Observable<? extends N1qlQueryResult>> ASYNC_RESULT_TO_SYNC = new Func1<AsyncN1qlQueryResult, Observable<N1qlQueryResult>>() {
        @Override
        public Observable<N1qlQueryResult> call(AsyncN1qlQueryResult aqr) {
            final boolean parseSuccess = aqr.parseSuccess();
            final String requestId = aqr.requestId();
            final String clientContextId = aqr.clientContextId();

            return Observable.zip(aqr.rows().toList(),
                    aqr.signature().singleOrDefault(JsonObject.empty()),
                    aqr.info().singleOrDefault(N1qlMetrics.EMPTY_METRICS),
                    aqr.errors().toList(),
                    aqr.profileInfo().singleOrDefault(JsonObject.empty()),
                    aqr.status(),
                    aqr.finalSuccess().singleOrDefault(Boolean.FALSE),
                    new Func7<List<AsyncN1qlQueryRow>, Object, N1qlMetrics, List<JsonObject>, JsonObject, String, Boolean, N1qlQueryResult>() {
                        @Override
                        public N1qlQueryResult call(List<AsyncN1qlQueryRow> rows, Object signature,
                                                    N1qlMetrics info, List<JsonObject> errors, JsonObject profileInfo, String finalStatus, Boolean finalSuccess) {
                            return new DefaultN1qlQueryResult(rows, signature, info, errors, profileInfo, finalStatus, finalSuccess,
                                    parseSuccess, requestId, clientContextId);
                        }
                    });
        }
    };

}
