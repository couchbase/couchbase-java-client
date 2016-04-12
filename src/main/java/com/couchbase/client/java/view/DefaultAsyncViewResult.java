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
