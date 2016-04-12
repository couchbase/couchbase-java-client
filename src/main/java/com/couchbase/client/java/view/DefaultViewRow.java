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

import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.util.Blocking;

import java.util.concurrent.TimeUnit;

public class DefaultViewRow implements ViewRow {

    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
    private final AsyncViewRow asyncViewRow;
    private final long timeout;

    public DefaultViewRow(CouchbaseEnvironment env, AsyncViewRow asyncViewRow) {
        this.asyncViewRow = asyncViewRow;
        this.timeout = env.kvTimeout();
    }

    @Override
    public String id() {
        return asyncViewRow.id();
    }

    @Override
    public Object key() {
        return asyncViewRow.key();
    }

    @Override
    public Object value() {
        return asyncViewRow.value();
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
        final StringBuilder sb = new StringBuilder("DefaultViewRow{");
        sb.append("id=").append(id());
        sb.append(", key=").append(key());
        sb.append(", value=").append(value());
        sb.append('}');
        return sb.toString();
    }
}
