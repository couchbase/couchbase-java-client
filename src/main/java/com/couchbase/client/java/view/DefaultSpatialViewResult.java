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
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.util.Blocking;
import rx.Observable;
import rx.functions.Func1;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of the {@link SpatialViewResult}.
 *
 * @author Michael Nitschinger
 * @since 2.1.0
 */
public class DefaultSpatialViewResult implements SpatialViewResult {

    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
    private final AsyncSpatialViewResult asyncViewResult;
    private final long timeout;
    private final CouchbaseEnvironment env;
    private final Bucket bucket;

    public DefaultSpatialViewResult(CouchbaseEnvironment env, Bucket bucket, Observable<AsyncSpatialViewRow> rows, boolean success, JsonObject error, JsonObject debug) {
        asyncViewResult = new DefaultAsyncSpatialViewResult(rows, success, error, debug);
        this.timeout = env.viewTimeout();
        this.env = env;
        this.bucket = bucket;
    }

    @Override
    public List<SpatialViewRow> allRows() {
        return allRows(timeout, TIMEOUT_UNIT);
    }

    @Override
    public List<SpatialViewRow> allRows(long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncViewResult
            .rows()
            .map(new Func1<AsyncSpatialViewRow, SpatialViewRow>() {
                @Override
                public SpatialViewRow call(AsyncSpatialViewRow asyncViewRow) {
                    return new DefaultSpatialViewRow(env, bucket, asyncViewRow.id(), asyncViewRow.key(), asyncViewRow.value(), asyncViewRow.geometry());
                }
            })
            .toList(), timeout, timeUnit);
    }

    @Override
    public Iterator<SpatialViewRow> rows() {
        return rows(timeout, TIMEOUT_UNIT);
    }

    @Override
    public Iterator<SpatialViewRow> rows(long timeout, TimeUnit timeUnit) {
        return asyncViewResult
            .rows()
            .map(new Func1<AsyncSpatialViewRow, SpatialViewRow>() {
                @Override
                public SpatialViewRow call(AsyncSpatialViewRow asyncViewRow) {
                    return new DefaultSpatialViewRow(env, bucket, asyncViewRow.id(), asyncViewRow.key(), asyncViewRow.value(), asyncViewRow.geometry());
                }
            })
            .timeout(timeout, timeUnit)
            .toBlocking()
            .getIterator();
    }

    @Override
    public boolean success() {
        return asyncViewResult.success();
    }

    @Override
    public JsonObject error() {
        return asyncViewResult.error();
    }

    @Override
    public JsonObject debug() {
        return asyncViewResult.debug();
    }

    @Override
    public Iterator<SpatialViewRow> iterator() {
        return rows();
    }
}
