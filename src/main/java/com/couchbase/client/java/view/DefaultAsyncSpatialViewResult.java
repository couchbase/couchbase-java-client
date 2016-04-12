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

/**
 * Default implementation of a {@link AsyncSpatialViewResult}.
 *
 * @author Michael Nitschinger
 * @since 2.1.0
 */
public class DefaultAsyncSpatialViewResult implements AsyncSpatialViewResult {

    private final Observable<AsyncSpatialViewRow> rows;
    private final boolean success;
    private final Observable<JsonObject> error;
    private final JsonObject debug;

    public DefaultAsyncSpatialViewResult(Observable<AsyncSpatialViewRow> rows, boolean success,
        Observable<JsonObject> error, JsonObject debug) {
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
    public Observable<JsonObject> error() {
        return error;
    }

    @Override
    public JsonObject debug() {
        return debug;
    }
}
