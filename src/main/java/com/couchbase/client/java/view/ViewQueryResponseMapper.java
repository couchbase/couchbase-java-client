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
package com.couchbase.client.java.view;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.view.ViewQueryResponse;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.CouchbaseAsyncBucket;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.document.json.JsonValue;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.error.ViewDoesNotExistException;
import com.couchbase.client.java.transcoder.JsonTranscoder;
import rx.Observable;
import rx.functions.Func1;

/**
 * Encapsulates functionality required to map from a {@link ViewQueryResponse} into a {@link AsyncViewResult}.
 *
 * @author Michael Nitschinger
 * @since 2.0.1
 */
@InterfaceStability.Committed
@InterfaceAudience.Private
public class ViewQueryResponseMapper {

    /**
     * The JSON transcoder used to convert to {@link JsonValue}s.
     */
    private static final JsonTranscoder TRANSCODER = CouchbaseAsyncBucket.JSON_OBJECT_TRANSCODER;

    private ViewQueryResponseMapper() {}

    /**
     * Maps a raw {@link ViewQueryResponse} into a {@link AsyncViewResult}.
     *
     * @param bucket reference to the bucket.
     * @param query the original query object.
     * @param response the response from the server.
     * @return a converted {@link AsyncViewResult}.
     */
    public static Observable<AsyncViewResult> mapToViewResult(final AsyncBucket bucket, final ViewQuery query,
        final ViewQueryResponse response) {

        return response
            .info()
            .singleOrDefault(null)
            .map(new ByteBufToJsonObject())
            .map(new BuildViewResult(bucket, query, response));
    }

    /**
     * Maps a raw {@link ViewQueryResponse} into a {@link AsyncSpatialViewResult}.
     *
     * @param bucket reference to the bucket.
     * @param query the original query object.
     * @param response the response from the server.
     * @return a converted {@link AsyncSpatialViewResult}.
     */
    public static Observable<AsyncSpatialViewResult> mapToSpatialViewResult(final AsyncBucket bucket,
        final SpatialViewQuery query, final ViewQueryResponse response) {

        return response
            .info()
            .singleOrDefault(null)
            .map(new ByteBufToJsonObject())
            .map(new BuildSpatialViewResult(bucket, query, response));
    }

    /**
     * Function which takes a {@link ByteBuf} and converts it into a {@link JsonObject}.
     */
    static class ByteBufToJsonObject implements Func1<ByteBuf, JsonObject> {

        @Override
        public JsonObject call(final ByteBuf input) {
            if (input == null || input.readableBytes() == 0) {
                return JsonObject.empty();
            }

            try {
                return TRANSCODER.byteBufToJsonObject(input);
            } catch (Exception e) {
                throw new TranscodingException("Could not decode View JSON: " + input.toString(CharsetUtil.UTF_8), e);
            } finally {
                if (input.refCnt() > 0) {
                    input.release();
                }
            }
        }

    }

    /**
     * Function which converts the {@link JsonObject} info into a {@link AsyncSpatialViewResult}.
     */
    static class BuildSpatialViewResult implements Func1<JsonObject, AsyncSpatialViewResult> {

        private final AsyncBucket bucket;
        private final SpatialViewQuery query;
        private final ViewQueryResponse response;

        BuildSpatialViewResult(AsyncBucket bucket, SpatialViewQuery query, ViewQueryResponse response) {
            this.bucket = bucket;
            this.query = query;
            this.response = response;
        }

        @Override
        public AsyncSpatialViewResult call(JsonObject jsonInfo) {
            JsonObject debug = null;
            boolean success = response.status().isSuccess();

            if (success) {
                debug = jsonInfo.getObject("debug_info");
            } else if (response.status() == ResponseStatus.NOT_EXISTS) {
                throw new ViewDoesNotExistException("View " + query.getDesign() + "/"
                    + query.getView() + " does not exist.");
            }

            Observable<AsyncSpatialViewRow> rows = response
                .rows()
                .map(new ByteBufToJsonObject())
                .flatMap(new Func1<JsonObject, Observable<AsyncSpatialViewRow>>() {
                    @Override
                    public Observable<AsyncSpatialViewRow> call(final JsonObject row) {
                        final String id = row.getString("id");

                        if (query.isIncludeDocs()) {
                            return bucket.get(id, query.includeDocsTarget()).map(new Func1<Document<?>, AsyncSpatialViewRow>() {
                                @Override
                                public AsyncSpatialViewRow call(Document<?> document) {
                                    return new DefaultAsyncSpatialViewRow(bucket, row.getString("id"), row.getArray("key"),
                                        row.get("value"), row.getObject("geometry"), document);
                                }
                            });
                        } else {
                            return Observable.just((AsyncSpatialViewRow)
                                new DefaultAsyncSpatialViewRow(bucket, row.getString("id"), row.getArray("key"),
                                    row.get("value"), row.getObject("geometry"), null)
                            );
                        }
                    }
                });

            Observable<JsonObject> error = response
                .error()
                .map(new Func1<String, JsonObject>() {
                    @Override
                    public JsonObject call(String input) {
                        try {
                            return TRANSCODER.stringToJsonObject(input);
                        } catch (Exception e) {
                            throw new TranscodingException("Could not decode View JSON: " + input, e);
                        }
                    }
                });

            return new DefaultAsyncSpatialViewResult(rows, success, error, debug);
        }

    }

    /**
     * Function which converts the {@link JsonObject} info into a {@link AsyncViewResult}.
     */
    static class BuildViewResult implements Func1<JsonObject, AsyncViewResult> {

        private final AsyncBucket bucket;
        private final ViewQuery query;
        private final ViewQueryResponse response;

        BuildViewResult(AsyncBucket bucket, ViewQuery query, ViewQueryResponse response) {
            this.bucket = bucket;
            this.query = query;
            this.response = response;
        }

        @Override
        public AsyncViewResult call(final JsonObject jsonInfo) {
            JsonObject debug = null;
            int totalRows = 0;
            boolean success = response.status().isSuccess();

            if (success) {
                debug = jsonInfo.getObject("debug_info");
                Integer trows = jsonInfo.getInt("total_rows");
                if (trows != null) {
                    totalRows = trows;
                }
            } else if (response.status() == ResponseStatus.NOT_EXISTS) {
                throw new ViewDoesNotExistException("View " + query.getDesign() + "/"
                    + query.getView() + " does not exist.");
            }

            Observable<AsyncViewRow> rows = response
                .rows()
                .map(new ByteBufToJsonObject())
                .compose(new Observable.Transformer<JsonObject, AsyncViewRow>() {
                    @Override
                    public Observable<AsyncViewRow> call(Observable<JsonObject> observable) {
                        if (!query.isIncludeDocs()) {
                            return observable.concatMap(buildAsyncViewRow());
                        } else if (query.isOrderRetained()) {
                            return observable.concatMapEager(buildAsyncViewRow());
                        } else {
                            return observable.flatMap(buildAsyncViewRow());
                        }
                    }
                });

            Observable<JsonObject> error = response
                .error()
                .map(new Func1<String, JsonObject>() {
                    @Override
                    public JsonObject call(String input) {
                        try {
                            return TRANSCODER.stringToJsonObject(input);
                        } catch (Exception e) {
                            throw new TranscodingException("Could not decode View JSON: " + input, e);
                        }
                    }
                });

            return new DefaultAsyncViewResult(rows, totalRows, success, error, debug);
        }

        private Func1<JsonObject, Observable<AsyncViewRow>> buildAsyncViewRow() {
            return new Func1<JsonObject, Observable<AsyncViewRow>>() {
                @Override
                public Observable<AsyncViewRow> call(final JsonObject row) {
                final String id = row.getString("id");

                if (query.isIncludeDocs()) {
                    return bucket.get(id, query.includeDocsTarget()).map(new Func1<Document<?>, AsyncViewRow>() {
                        @Override
                        public AsyncViewRow call(Document<?> document) {
                            return new DefaultAsyncViewRow(bucket, id, row.get("key"), row.get("value"), document);
                        }
                    });
                } else {
                    return Observable.just((AsyncViewRow)
                                    new DefaultAsyncViewRow(bucket, id, row.get("key"), row.get("value"), null)
                    );
                }
                }
            };
        }

    }

}
