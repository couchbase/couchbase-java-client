/*
 * Copyright (c) 2017 Couchbase, Inc.
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
package com.couchbase.client.java.analytics;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.message.analytics.GenericAnalyticsRequest;
import com.couchbase.client.core.message.analytics.GenericAnalyticsResponse;
import com.couchbase.client.core.time.Delay;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.bucket.api.Utils;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.error.CannotRetryException;
import com.couchbase.client.java.error.QueryExecutionException;
import com.couchbase.client.java.error.TemporaryFailureException;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.transcoder.TranscoderUtils;
import com.couchbase.client.java.util.retry.RetryBuilder;
import io.opentracing.tag.Tags;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action4;
import rx.functions.Func1;
import rx.functions.Func5;
import rx.functions.Func6;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.couchbase.client.java.CouchbaseAsyncBucket.JSON_OBJECT_TRANSCODER;
import static com.couchbase.client.java.bucket.api.Utils.applyTimeout;
import static com.couchbase.client.java.util.OnSubscribeDeferAndWatch.deferAndWatch;

public class AnalyticsQueryExecutor {

    /**
     * The logger used.
     */
    private static CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(AnalyticsQueryExecutor.class);

    private static final String ERROR_FIELD_CODE = "code";

    private final ClusterFacade core;
    private final String bucket;
    private final String username;
    private final String password;

    public AnalyticsQueryExecutor(ClusterFacade core, String bucket, String username, String password) {
        this.core = core;
        this.bucket = bucket;
        this.username = username;
        this.password = password;
    }

    public Observable<AsyncAnalyticsQueryResult> execute(final AnalyticsQuery query, final CouchbaseEnvironment env,
                                                         final long timeout, final TimeUnit timeUnit) {
        return deferAndWatch(new Func1<Subscriber, Observable<GenericAnalyticsResponse>>() {
            @Override
            public Observable<GenericAnalyticsResponse> call(final Subscriber subscriber) {
                GenericAnalyticsRequest request = GenericAnalyticsRequest
                    .jsonQuery(query.query().toString(), bucket, username, password, query.params().priority());
                Utils.addRequestSpan(env, request, "analytics");
                if (env.operationTracingEnabled()) {
                    request.span().setTag(Tags.DB_STATEMENT.getKey(), query.statement());
                }
                request.subscriber(subscriber);
                return applyTimeout(core.<GenericAnalyticsResponse>send(request), request, env, timeout, timeUnit);
            }
        }).flatMap(new Func1<GenericAnalyticsResponse, Observable<AsyncAnalyticsQueryResult>>() {
            @Override
            public Observable<AsyncAnalyticsQueryResult> call(final GenericAnalyticsResponse response) {

                final Observable<Object> signature = response.signature().map(new Func1<ByteBuf, Object>() {
                    @Override
                    public Object call(ByteBuf byteBuf) {
                        try {
                            return JSON_OBJECT_TRANSCODER.byteBufJsonValueToObject(byteBuf);
                        } catch (Exception e) {
                            throw new TranscodingException("Could not decode Analytics Query Signature", e);
                        } finally {
                            byteBuf.release();
                        }
                    }
                });
                final Observable<AnalyticsMetrics> info = response.info().map(new Func1<ByteBuf, JsonObject>() {
                    @Override
                    public JsonObject call(ByteBuf byteBuf) {
                        try {
                            return JSON_OBJECT_TRANSCODER.byteBufToJsonObject(byteBuf);
                        } catch (Exception e) {
                            throw new TranscodingException("Could not decode Analytics Query Metrics.", e);
                        } finally {
                            byteBuf.release();
                        }
                    }
                })
                    .map(new Func1<JsonObject, AnalyticsMetrics>() {
                        @Override
                        public AnalyticsMetrics call(JsonObject jsonObject) {
                            return new AnalyticsMetrics(jsonObject);
                        }
                    });
                final Observable<String> finalStatus = response.queryStatus();
                final Observable<JsonObject> errors = response.errors().map(new Func1<ByteBuf, JsonObject>() {
                    @Override
                    public JsonObject call(ByteBuf byteBuf) {
                        try {
                            return JSON_OBJECT_TRANSCODER.byteBufToJsonObject(byteBuf);
                        } catch (Exception e) {
                            throw new TranscodingException("Could not decode Analytics Errors.", e);
                        } finally {
                            byteBuf.release();
                        }
                    }
                });
                boolean parseSuccess = response.status().isSuccess();
                String contextId = response.clientRequestId() == null ? "" : response.clientRequestId();
                String requestId = response.requestId();
                if (!query.params().deferred()) {
                    final Observable<AsyncAnalyticsQueryRow> rows = response.rows().map(new Func1<ByteBuf, AsyncAnalyticsQueryRow>() {
                        @Override
                        public AsyncAnalyticsQueryRow call(ByteBuf byteBuf) {
                            try {
                                TranscoderUtils.ByteBufToArray rawData = TranscoderUtils.byteBufToByteArray(byteBuf);
                                byte[] copy = Arrays.copyOfRange(rawData.byteArray, rawData.offset, rawData.offset + rawData.length);
                                return new DefaultAsyncAnalyticsQueryRow(copy);
                            } catch (Exception e) {
                                throw new TranscodingException("Could not decode Analytics Query Row.", e);
                            } finally {
                                byteBuf.release();
                            }
                        }
                    });
                    AsyncAnalyticsQueryResult r = new DefaultAsyncAnalyticsQueryResult(rows, signature, info, errors,
                            finalStatus, parseSuccess, requestId, contextId);
                    return Observable.just(r);
                } else {
                    String statusHandleStr = response.handle();
                    AsyncAnalyticsDeferredResultHandle handle = new DefaultAsyncAnalyticsDeferredResultHandle(statusHandleStr, env, core, bucket, username, password, timeout, timeUnit);
                    AsyncAnalyticsQueryResult r = new DefaultAsyncAnalyticsQueryResult(handle, signature, info, errors,
                            finalStatus, parseSuccess, requestId, contextId);
                    return Observable.just(r);
                }
            }
        })
        .flatMap(RESULT_PEEK_FOR_RETRY)
        .retryWhen(RetryBuilder
            .anyOf(AnalyticsTemporaryFailureException.class)
            .delay(Delay.exponential(TimeUnit.MILLISECONDS, 500, 2))
            .max(10)
            .doOnRetry(new Action4<Integer, Throwable, Long, TimeUnit>() {
                @Override
                public void call(Integer attempt, Throwable error, Long delay, TimeUnit delayUnit) {
                    LOGGER.debug("Retrying {} because of {} (attempt {}, delay {} {})", query.query(),
                            error.getMessage(), attempt, delay, delayUnit);
                }
            })
            .build()
        )
        .onErrorResumeNext(new Func1<Throwable, Observable<? extends AsyncAnalyticsQueryResult>>() {
            @Override
            public Observable<? extends AsyncAnalyticsQueryResult> call(Throwable throwable) {
                if (throwable instanceof CannotRetryException) {
                    if (throwable.getCause() != null && throwable.getCause() instanceof AnalyticsTemporaryFailureException) {
                        AnalyticsTemporaryFailureException x = (AnalyticsTemporaryFailureException) throwable.getCause();
                        return Observable.just(x.result());
                    }
                }
                return Observable.error(throwable);
            }
        });
    }

    /**
     * A function that can be used in a flatMap to convert an {@link AsyncAnalyticsQueryResult} to
     * a {@link AnalyticsQueryResult}.
     *
     */
    public static final Func1<? super AsyncAnalyticsQueryResult, ? extends Observable<? extends AnalyticsQueryResult>> ASYNC_RESULT_TO_SYNC = new Func1<AsyncAnalyticsQueryResult, Observable<AnalyticsQueryResult>>() {
        @Override
        public Observable<AnalyticsQueryResult> call(AsyncAnalyticsQueryResult aqr) {
            final boolean parseSuccess = aqr.parseSuccess();
            final String requestId = aqr.requestId();
            final String clientContextId = aqr.clientContextId();

            return Observable.zip(aqr.rows().toList(),
                aqr.signature().singleOrDefault(JsonObject.empty()),
                aqr.info().singleOrDefault(AnalyticsMetrics.EMPTY_METRICS),
                aqr.errors().toList(),
                aqr.status(),
                aqr.finalSuccess().singleOrDefault(Boolean.FALSE),
                new Func6<List<AsyncAnalyticsQueryRow>, Object, AnalyticsMetrics, List<JsonObject>, String, Boolean, AnalyticsQueryResult>() {
                    @Override
                    public AnalyticsQueryResult call(List<AsyncAnalyticsQueryRow> rows, Object signature,
                        AnalyticsMetrics info, List<JsonObject> errors, String finalStatus, Boolean finalSuccess) {
                        return new DefaultAnalyticsQueryResult(rows, signature, info, errors, finalStatus, finalSuccess,
                            parseSuccess, requestId, clientContextId);
                    }
                });
        }
    };

    /**
     * A function that can be used in a flatMap to convert an {@link AsyncAnalyticsQueryResult} to
     * a {@link AnalyticsQueryResult} for deferred queries.
     *
     */
    public static final Func1<? super AsyncAnalyticsQueryResult, ? extends Observable<? extends AnalyticsQueryResult>> ASYNC_RESULT_TO_SYNC_DEFERRED = new Func1<AsyncAnalyticsQueryResult, Observable<AnalyticsQueryResult>>() {
        @Override
        public Observable<AnalyticsQueryResult> call(final AsyncAnalyticsQueryResult aqr) {
            final boolean parseSuccess = aqr.parseSuccess();
            final String requestId = aqr.requestId();
            final String clientContextId = aqr.clientContextId();

            return Observable.zip(aqr.signature().singleOrDefault(JsonObject.empty()),
                    aqr.info().singleOrDefault(AnalyticsMetrics.EMPTY_METRICS),
                    aqr.errors().toList(),
                    aqr.status(),
                    aqr.finalSuccess().singleOrDefault(Boolean.FALSE),
                    new Func5<Object, AnalyticsMetrics, List<JsonObject>, String, Boolean, AnalyticsQueryResult>() {
                        @Override
                        public AnalyticsQueryResult call(Object signature, AnalyticsMetrics info, List<JsonObject> errors, String finalStatus, Boolean finalSuccess) {
                            return new DefaultAnalyticsQueryResult(aqr.handle(), signature, info, errors, finalStatus, finalSuccess,
                                    parseSuccess, requestId, clientContextId);
                        }
                    });
        }
    };

    protected static final Func1<AsyncAnalyticsQueryResult, Observable<AsyncAnalyticsQueryResult>> RESULT_PEEK_FOR_RETRY =
            new Func1<AsyncAnalyticsQueryResult, Observable<AsyncAnalyticsQueryResult>>() {
                @Override
                public Observable<AsyncAnalyticsQueryResult> call(final AsyncAnalyticsQueryResult aqr) {
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
                                .flatMap(new Func1<JsonObject, Observable<AsyncAnalyticsQueryResult>>() {
                                    @Override
                                    public Observable<AsyncAnalyticsQueryResult> call(JsonObject errorJson) {
                                        AsyncAnalyticsQueryResult copyResult;
                                        if (aqr.handle() != null) {
                                            copyResult = new DefaultAsyncAnalyticsQueryResult(
                                                    aqr.handle(),
                                                    aqr.signature(),
                                                    aqr.info(),
                                                    cachedErrors,
                                                    aqr.status(),
                                                    aqr.parseSuccess(),
                                                    aqr.requestId(),
                                                    aqr.clientContextId()
                                            );
                                        } else {
                                            copyResult = new DefaultAsyncAnalyticsQueryResult(
                                                    aqr.rows(),
                                                    aqr.signature(),
                                                    aqr.info(),
                                                    cachedErrors,
                                                    aqr.status(),
                                                    aqr.parseSuccess(),
                                                    aqr.requestId(),
                                                    aqr.clientContextId()
                                            );
                                        }
                                        if (errorJson == null) {
                                            return Observable.just(copyResult);
                                        } else {
                                            return Observable.error(
                                                new AnalyticsTemporaryFailureException(copyResult)
                                            );
                                        }
                                    }
                                });
                    } else {
                        return Observable.just(aqr);
                    }
                }
            };

    private static boolean shouldRetry(final JsonObject errorJson) {
        if (errorJson == null) return false;
        Integer code = errorJson.getInt(ERROR_FIELD_CODE);

        // The following error codes have been identified as being
        // retryable.
        switch (code) {
            case 21002:
            case 23000:
            case 23003:
            case 23007:
                return true;
            default:
                return false;
        }
    }

    static class AnalyticsTemporaryFailureException extends TemporaryFailureException {
        private final AsyncAnalyticsQueryResult result;

        public AnalyticsTemporaryFailureException(AsyncAnalyticsQueryResult result) {
            this.result = result;
        }

        public AsyncAnalyticsQueryResult result() {
            return result;
        }
    }
}
