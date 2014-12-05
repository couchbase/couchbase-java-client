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

/**
 * Default implementation of a {@link AsyncSpatialViewResult}.
 *
 * @author Michael Nitschinger
 * @since 2.1.0
 */
public class DefaultAsyncSpatialViewResult implements AsyncSpatialViewResult {

    private final Observable<AsyncSpatialViewRow> rows;
    private final boolean success;
    private final JsonObject error;
    private final JsonObject debug;

    public DefaultAsyncSpatialViewResult(Observable<AsyncSpatialViewRow> rows, boolean success, JsonObject error, JsonObject debug) {
        this.rows = rows;
        this.success = success;
        this.error = error;
        this.debug = debug;
    }

    @Override
    public Observable<AsyncSpatialViewRow> rows() {
        return rows;
    }

    @Override
    public boolean success() {
        return success;
    }

    @Override
    public JsonObject error() {
        return error;
    }

    @Override
    public JsonObject debug() {
        return debug;
    }
}
