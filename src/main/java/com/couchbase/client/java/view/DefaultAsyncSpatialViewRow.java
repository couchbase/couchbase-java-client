/**
 * Copyright (C) 2014 Couchbase, Inc.
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
