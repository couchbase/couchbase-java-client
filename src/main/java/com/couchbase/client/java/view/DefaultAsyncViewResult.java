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

import com.couchbase.client.java.document.json.JsonObject;
import rx.Observable;

public class DefaultAsyncViewResult implements AsyncViewResult {

    private final Observable<AsyncViewRow> rows;
    private final int totalRows;
    private final boolean success;
    private final Observable<JsonObject> error;
    private final JsonObject debug;

    public DefaultAsyncViewResult(Observable<AsyncViewRow> rows, int totalRows, boolean success,
        Observable<JsonObject> error, JsonObject debug) {
        this.rows = rows;
        this.totalRows = totalRows;
        this.success = success;
        this.error = error;
        this.debug = debug;
    }

    @Override
    public Observable<AsyncViewRow> rows() {
        return rows;
    }

    @Override
    public int totalRows() {
        return totalRows;
    }

    @Override
    public boolean success() {
        return success;
    }

    @Override
    public Observable<JsonObject> error() {
        return error;
    }

    @Override
    public JsonObject debug() {
        return debug;
    }
}
