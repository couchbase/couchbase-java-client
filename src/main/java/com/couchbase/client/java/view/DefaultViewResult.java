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

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.util.Blocking;
import rx.Observable;
import rx.functions.Func1;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DefaultViewResult implements ViewResult {

    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
    private AsyncViewResult asyncViewResult;
    private final long timeout;
    private final CouchbaseEnvironment env;
    private final Bucket bucket;

    public DefaultViewResult(CouchbaseEnvironment env, Bucket bucket, Observable<AsyncViewRow>rows, int totalRows, boolean success, Observable<JsonObject> error, JsonObject debug) {
        asyncViewResult = new DefaultAsyncViewResult(rows, totalRows, success, error, debug);
        this.timeout = env.viewTimeout();
        this.env = env;
        this.bucket = bucket;
    }

    @Override
    public List<ViewRow> allRows() {
        return allRows(timeout, TIMEOUT_UNIT);
    }

    @Override
    public List<ViewRow> allRows(long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncViewResult
            .rows()
            .map(new Func1<AsyncViewRow, ViewRow>() {
                @Override
                public ViewRow call(AsyncViewRow asyncViewRow) {
                    return new DefaultViewRow(env, asyncViewRow);
                }
            })
            .toList(), timeout, timeUnit);
    }

    @Override
    public Iterator<ViewRow> rows() {
        return rows(timeout, TIMEOUT_UNIT);
    }

    @Override
    public Iterator<ViewRow> rows(long timeout, TimeUnit timeUnit) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("Timeout must be greater than 0");
        }

        return asyncViewResult
            .rows()
            .map(new Func1<AsyncViewRow, ViewRow>() {
                @Override
                public ViewRow call(AsyncViewRow asyncViewRow) {
                    return new DefaultViewRow(env, asyncViewRow);
                }
            })
            .timeout(timeout, timeUnit)
            .toBlocking()
            .getIterator();
    }

    @Override
    public Iterator<ViewRow> iterator() {
        return rows();
    }

    @Override
    public int totalRows() {
        return asyncViewResult.totalRows();
    }

    @Override
    public boolean success() {
        return asyncViewResult.success();
    }

    @Override
    public JsonObject error() {
        return error(timeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonObject error(long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncViewResult.error(), timeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonObject debug() {
        return asyncViewResult.debug();
    }
}
