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
import com.couchbase.client.core.message.analytics.GenericAnalyticsRequest;
import com.couchbase.client.core.message.analytics.GenericAnalyticsResponse;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.transcoder.TranscoderUtils;
import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func6;

import java.util.Arrays;
import java.util.List;

import static com.couchbase.client.java.CouchbaseAsyncBucket.JSON_OBJECT_TRANSCODER;

public class AnalyticsQueryExecutor {

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

    public Observable<AsyncAnalyticsQueryResult> execute(final AnalyticsQuery query) {
        return Observable.defer(new Func0<Observable<GenericAnalyticsResponse>>() {
            @Override
            public Observable<GenericAnalyticsResponse> call() {
                return core.send(GenericAnalyticsRequest.jsonQuery(query.query().toString(), bucket, username, password));
            }
        }).flatMap(new Func1<GenericAnalyticsResponse, Observable<AsyncAnalyticsQueryResult>>() {
            @Override
            public Observable<AsyncAnalyticsQueryResult> call(final GenericAnalyticsResponse response) {
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

                AsyncAnalyticsQueryResult r = new DefaultAsyncAnalyticsQueryResult(rows, signature, info, errors,
                    finalStatus, parseSuccess, requestId, contextId);
                return Observable.just(r);            }
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
}
