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
import rx.Observable;

/**
 * Default implementation of a {@link AsyncViewRow}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class DefaultAsyncViewRow implements AsyncViewRow {

    private final String id;
    private final Object key;
    private final Object value;
    private final AsyncBucket bucket;
    private final Document<?> document;

    public DefaultAsyncViewRow(AsyncBucket bucket, String id, Object key, Object value, Document<?> document) {
        this.bucket = bucket;
        this.id = id;
        this.key = key;
        this.value = value;
        this.document = document;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Object key() {
        return key;
    }

    @Override
    public Object value() {
        return value;
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
        final StringBuilder sb = new StringBuilder("DefaultViewRow{");
        sb.append("id='").append(id).append('\'');
        sb.append(", key=").append(key);
        sb.append(", value=").append(value);
        sb.append(", document=").append(document);
        sb.append('}');
        return sb.toString();
    }
}
