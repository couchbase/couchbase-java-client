/**
 * Copyright (C) 2015 Couchbase, Inc.
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
package com.couchbase.client.java.repository;

import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.repository.mapping.DefaultEntityConverter;
import com.couchbase.client.java.repository.mapping.EntityConverter;
import rx.Observable;
import rx.functions.Func1;

public class CouchbaseAsyncRepository implements AsyncRepository {

    private final EntityConverter converter;
    private final AsyncBucket bucket;

    public CouchbaseAsyncRepository(AsyncBucket bucket) {
        this.bucket = bucket;
        converter = new DefaultEntityConverter();
    }

    @Override
    public <T> Observable<T> get(String id, final Class<T> documentClass) {
        return Observable
            .just(id)
            .flatMap(new Func1<String, Observable<JsonDocument>>() {
                @Override
                public Observable<JsonDocument> call(String id) {
                    return bucket.get(id);
                }
            })
            .map(new Func1<JsonDocument, T>() {
                @Override
                public T call(JsonDocument document) {
                    return (T) converter.toEntity(document, documentClass);
                }
            });
    }

    @Override
    public <T> Observable<T> upsert(final T document) {
        return Observable
            .just(document)
            .flatMap(new Func1<T, Observable<? extends Document<?>>>() {
                @Override
                public Observable<? extends Document<?>> call(T source) {
                    Document<?> converted = converter.fromEntity(source);
                    return bucket.upsert(converted);
                }
            }).map(new Func1<Document<?>, T>() {
                @Override
                public T call(Document<?> stored) {
                    return document;
                }
            });
    }
}
