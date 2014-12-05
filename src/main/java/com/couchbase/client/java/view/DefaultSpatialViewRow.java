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

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.util.Blocking;

import java.util.concurrent.TimeUnit;

/**
 * Default implementation of the {@link SpatialViewRow}.
 *
 * @author Michael Nitschinger
 * @since 2.1.0
 */
public class DefaultSpatialViewRow implements SpatialViewRow {

    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
    private final AsyncSpatialViewRow asyncViewRow;
    private final long timeout;

    public DefaultSpatialViewRow(CouchbaseEnvironment env, Bucket bucket, String id, JsonArray key, Object value, JsonObject geometry) {
        this.asyncViewRow = new DefaultAsyncSpatialViewRow(bucket.async(), id, key, value, geometry);
        this.timeout = env.kvTimeout();
    }

    @Override
    public String id() {
        return asyncViewRow.id();
    }

    @Override
    public JsonArray key() {
        return asyncViewRow.key();
    }

    @Override
    public Object value() {
        return asyncViewRow.value();
    }

    @Override
    public JsonObject geometry() {
        return asyncViewRow.geometry();
    }

    @Override
    public JsonDocument document() {
        return document(timeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonDocument document(long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncViewRow.document().singleOrDefault(null), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D document(Class<D> target) {
        return document(target, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D document(Class<D> target, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncViewRow.document(target).singleOrDefault(null), timeout, timeUnit);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DefaultSpatialViewRow{");
        sb.append("id=").append(id());
        sb.append(", key=").append(key());
        sb.append(", value=").append(value());
        sb.append(", geometry=").append(geometry());
        sb.append('}');
        return sb.toString();
    }
}
