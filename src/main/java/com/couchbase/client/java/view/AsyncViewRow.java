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

import com.couchbase.client.core.BackpressureException;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.TranscodingException;
import rx.Observable;

/**
 * Represents a row fetched from the {@link View}.
 *
 * The row itself contains fixed properties returned, but is also able to - on demand - load the full document if
 * instructed through the {@link #document()} methods.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface AsyncViewRow {

    /**
     * The id of the document, if not reduced.
     *
     * @return the id of the document.
     */
    String id();

    /**
     * The key of the row index.
     *
     * The object can be any valid JSON object, including {@link JsonArray} or {@link JsonObject}.
     *
     * @return the key.
     */
    Object key();

    /**
     * The value of the row index.
     *
     * The object can be any valid JSON object, including {@link JsonArray} or {@link JsonObject}.
     *
     * @return the value.
     */
    Object value();

    /**
     * Load the underlying document, if not reduced.
     *
     * The {@link Observable} can error under the following conditions:
     *
     *  - {@link BackpressureException}: If the incoming request rate is too high to be processed.
     *  - {@link IllegalStateException}: If the view is reduced and the ID is null.
     *  - {@link TranscodingException}: If the response document could not be decoded.
     *
     * @return a {@link Observable} containing the document once loaded.
     */
    Observable<JsonDocument> document();

    /**
     * Load the underlying document, if not reduced.
     *
     * The {@link Observable} can error under the following conditions:
     *
     *  - {@link BackpressureException}: If the incoming request rate is too high to be processed.
     *  - {@link IllegalStateException}: If the view is reduced and the ID is null.
     *  - {@link TranscodingException}: If the response document could not be decoded.
     *
     * @param target the target class to decode into.
     * @return a {@link Observable} containing the document once loaded.
     */
    <D extends Document<?>> Observable<D> document(final Class<D> target);

}
