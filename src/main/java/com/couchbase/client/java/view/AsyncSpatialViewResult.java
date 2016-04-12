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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonObject;
import rx.Observable;

/**
 * Contains the result of a {@link SpatialViewQuery}.
 *
 * @author Michael Nitschinger
 * @since 2.1.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface AsyncSpatialViewResult {

    /**
     * Emits one {@link AsyncViewRow} for each row received from the view.
     *
     * @return a (potentially empty) {@link Observable} containing view rows.
     */
    Observable<AsyncSpatialViewRow> rows();

    /**
     * If the query was successful.
     *
     * @return true if it was, false otherwise.
     */
    boolean success();

    /**
     * If it was not successful, an error is contained here.
     *
     * @return the potential error.
     */
    Observable<JsonObject> error();

    /**
     * If debug was enabled on the query, it is contained here.
     *
     * @return the debug info.
     */
    JsonObject debug();
}
