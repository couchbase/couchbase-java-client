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

import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import rx.Observable;

/**
 * Default implementation of a {@link AsyncSpatialViewRow}.
 *
 * @author Michael Nitschinger
 * @since 2.1.0
 */
public class DefaultAsyncSpatialViewRow implements AsyncSpatialViewRow {

    private final String id;
    private final JsonArray key;
    private final Object value;
    private final AsyncBucket bucket;
    private final JsonObject geometry;
    private final Document<?> document;

    public DefaultAsyncSpatialViewRow(AsyncBucket bucket, String id, JsonArray key, Object value, JsonObject geometry, Document<?> document) {
        this.bucket = bucket;
        this.id = id;
        this.key = key;
        this.value = value;
        this.geometry = geometry;
        this.document = document;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public JsonArray key() {
        return key;
    }

    @Override
    public Object value() {
        return value;
    }

    @Override
    public JsonObject geometry() {
        return geometry;
    }

    @Override
    public Observable<JsonDocument> document() {
        return document(JsonDocument.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> document(Class<D> target) {
        if (document != null) {
            return Observable.just((D) document);
        }
        if (id == null) {
            return Observable.error(new UnsupportedOperationException("Document cannot be loaded, id is null."));
        }
        return bucket.get(id, target);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AsyncSpatialViewRow{");
        sb.append("id='").append(id).append('\'');
        sb.append(", key=").append(key);
        sb.append(", value=").append(value);
        sb.append(", geometry=").append(geometry);
        sb.append(", document=").append(document);
        sb.append('}');
        return sb.toString();
    }
}
