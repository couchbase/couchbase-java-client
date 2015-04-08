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

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.util.Blocking;

import java.util.concurrent.TimeUnit;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class CouchbaseRepository implements Repository {

    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

    private final AsyncRepository asyncRepository;
    private final Bucket bucket;
    private final long timeout;

    public CouchbaseRepository(Bucket bucket, CouchbaseEnvironment environment) {
        this.bucket = bucket;
        this.timeout = environment.kvTimeout();
        this.asyncRepository = bucket.async().repository().toBlocking().single();
    }

    @Override
    public AsyncRepository async() {
        return asyncRepository;
    }

    @Override
    public <T> T get(String id, Class<T> entityClass) {
        return get(id, entityClass, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> T get(String id, Class<T> entityClass, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncRepository.get(id, entityClass), timeout, timeUnit);
    }

    @Override
    public <T> T upsert(T document) {
        return upsert(document, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> T upsert(T document, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncRepository.upsert(document), timeout, timeUnit);
    }

}
