/*
 * Copyright (c) 2018 Couchbase, Inc.
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

import static com.couchbase.client.java.bucket.api.Utils.applyTimeout;
import static com.couchbase.client.java.util.OnSubscribeDeferAndWatch.deferAndWatch;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.message.analytics.AnalyticsQueryResultRequest;
import com.couchbase.client.core.message.analytics.AnalyticsQueryStatusRequest;
import com.couchbase.client.core.message.analytics.GenericAnalyticsResponse;
import com.couchbase.client.core.time.Delay;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.error.CannotRetryException;
import com.couchbase.client.java.error.QueryExecutionException;
import com.couchbase.client.java.error.TemporaryFailureException;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.transcoder.TranscoderUtils;
import com.couchbase.client.java.util.retry.RetryBuilder;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action4;
import rx.functions.Func1;

/**
 * Default implementation of {@link AsyncAnalyticsDeferredResultHandle}
 *
 * @author Subhashni Balakrishnan
 * @since 2.7.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class DefaultAsyncAnalyticsDeferredResultHandle implements AsyncAnalyticsDeferredResultHandle {

    private static CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(DefaultAsyncAnalyticsDeferredResultHandle.class);

    private final CouchbaseEnvironment env;
    private final ClusterFacade core;
    private final String bucket;
    private final String username;
    private final String password;
    private final String statusHandle;
    private String resultHandle;
    private final long timeout;
    private final TimeUnit timeunit;

    public DefaultAsyncAnalyticsDeferredResultHandle(String handle, final CouchbaseEnvironment env, final ClusterFacade core,
                                       final String bucket, final String username, final String password,
                                       final long timeout, final TimeUnit timeunit) {
        this.statusHandle = handle;
        this.resultHandle = "";
        this.env = env;
        this.core = core;
        this.bucket = bucket;
        this.username = username;
        this.password = password;
        this.timeout = timeout;
        this.timeunit = timeunit;
    }

    @Override
    public String getStatusHandleUri() {
        return this.statusHandle;
    }

    @Override
    public String getResultHandleUri() {
        if (this.resultHandle.length() == 0) {
            throw new IllegalStateException("There is no result handle available, retry status until success");
        }
        return this.resultHandle;
    }

    @Override
    public Observable<AsyncAnalyticsQueryRow> rows() {
        return this.rows(this.timeout, this.timeunit);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable<AsyncAnalyticsQueryRow> rows(final long timeout, final TimeUnit timeunit) {
        if (this.resultHandle.length() == 0) {
            throw new QueryExecutionException("There is no result handle available to fetch rows, retry status call until success", null);
        }
        return deferAndWatch(new Func1<Subscriber, Observable<GenericAnalyticsResponse>>() {
            @Override
            public Observable<GenericAnalyticsResponse> call(final Subscriber subscriber) {
                AnalyticsQueryResultRequest request = new AnalyticsQueryResultRequest(resultHandle, bucket, username, password);
                request.subscriber(subscriber);
                return applyTimeout(core.<GenericAnalyticsResponse>send(request), request, env, timeout, timeunit);
            }
        }).flatMap(new Func1<GenericAnalyticsResponse, Observable<AsyncAnalyticsQueryRow>>() {
            @Override
            public Observable<AsyncAnalyticsQueryRow> call(final GenericAnalyticsResponse response) {
                return response.rows().map(new Func1<ByteBuf, AsyncAnalyticsQueryRow>() {
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
            }
        }).retryWhen(RetryBuilder.anyOf(TemporaryFailureException.class)
                        .delay(Delay.exponential(TimeUnit.MILLISECONDS, 500, 2))
                        .max(10)
                        .doOnRetry(new Action4<Integer, Throwable, Long, TimeUnit>() {
                            @Override
                            public void call(Integer attempt, Throwable error, Long delay, TimeUnit delayUnit) {
                                LOGGER.debug("Retrying status because of temp failure (attempt {}, delay {} {})", error.getMessage(), attempt, delay, delayUnit);
                            }
                        })
                        .build()
        ).onErrorResumeNext(new Func1<Throwable, Observable<? extends AsyncAnalyticsQueryRow>>() {
                    @Override
                    public Observable<? extends AsyncAnalyticsQueryRow> call(Throwable throwable) {
                        if (throwable instanceof CannotRetryException) {
                            Observable.empty();
                        }
                        return Observable.error(throwable);
                    }
        });
    }

    @Override
    public Observable<String> status() {
        return this.status(this.timeout, this.timeunit);
    }

    @Override
    public Observable<String> status(final long timeout, final TimeUnit timeunit) {
        return deferAndWatch(new Func1<Subscriber, Observable<GenericAnalyticsResponse>>() {
            @Override
            public Observable<GenericAnalyticsResponse> call(final Subscriber subscriber) {
                AnalyticsQueryStatusRequest request = new AnalyticsQueryStatusRequest(statusHandle, bucket, username, password);
                request.subscriber(subscriber);
                return applyTimeout(core.<GenericAnalyticsResponse>send(request), request, env, timeout, timeunit);
            }
        }).flatMap(new Func1<GenericAnalyticsResponse, Observable<String>>() {
            @Override
            public Observable<String> call(final GenericAnalyticsResponse response) {
                resultHandle = response.handle();
                return response.queryStatus();
            }
        }).retryWhen(RetryBuilder.anyOf(TemporaryFailureException.class)
                .delay(Delay.exponential(TimeUnit.MILLISECONDS, 500, 2))
                .max(10)
                .doOnRetry(new Action4<Integer, Throwable, Long, TimeUnit>() {
                    @Override
                    public void call(Integer attempt, Throwable error, Long delay, TimeUnit delayUnit) {
                        LOGGER.debug("Retrying status because of temp failure (attempt {}, delay {} {})", error.getMessage(), attempt, delay, delayUnit);
                    }
                })
                .build()
        ).onErrorResumeNext(new Func1<Throwable, Observable<String>>() {
            @Override
            public Observable<String> call(Throwable throwable) {
                if (throwable instanceof CannotRetryException) {
                    Observable.empty();
                }
                return Observable.error(throwable);
            }
        });
    }

    @Override
    public String toString() {
        return "DefaultAsyncAnalyticsDeferredResultHandle{" +
                "statusUri='" + getStatusHandleUri() + '\'' +
                ", resultUri='" + getResultHandleUri() + '\'' +
                '}';
    }
}
