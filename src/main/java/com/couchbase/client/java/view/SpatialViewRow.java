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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Represents a {@link SpatialViewRow} fetched from the View.
 *
 * @author Michael Nitschinger
 * @since 2.1.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface SpatialViewRow {

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
    JsonArray key();

    /**
     * The value of the row index.
     *
     * The object can be any valid JSON object, including {@link JsonArray} or {@link JsonObject}.
     *
     * @return the value.
     */
    Object value();


    JsonObject geometry();

    /**
     * Load the underlying document, if not reduced with the default view timeout.
     *
     * This method throws:
     *
     *  - {@link TimeoutException} wrapped in a {@link RuntimeException}: If the timeout is exceeded.
     *  - {@link BackpressureException}: If the incoming request rate is too high to be processed.
     *  - {@link IllegalStateException}: If the view is reduced and the ID is null.
     *  - {@link TranscodingException}: If the response document could not be decoded.
     *
     * @return the loaded document, null if not found.
     */
    JsonDocument document();

    /**
     * Load the underlying document, if not reduced with a custom timeout.
     *
     * This method throws:
     *
     *  - {@link TimeoutException} wrapped in a {@link RuntimeException}: If the timeout is exceeded.
     *  - {@link BackpressureException}: If the incoming request rate is too high to be processed.
     *  - {@link IllegalStateException}: If the view is reduced and the ID is null.
     *  - {@link TranscodingException}: If the response document could not be decoded.
     *
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return the loaded document, null if not found.
     */
    JsonDocument document(long timeout, TimeUnit timeUnit);

    /**
     * Load the underlying document, if not reduced with the default view timeout.
     *
     * This method throws:
     *
     *  - {@link TimeoutException} wrapped in a {@link RuntimeException}: If the timeout is exceeded.
     *  - {@link BackpressureException}: If the incoming request rate is too high to be processed.
     *  - {@link IllegalStateException}: If the view is reduced and the ID is null.
     *  - {@link TranscodingException}: If the response document could not be decoded.
     *
     * @param target the custom target document type.
     * @return the loaded document, null if not found.
     */
    <D extends Document<?>> D document(final Class<D> target);

    /**
     * Load the underlying document, if not reduced with a custom timeout.
     *
     * This method throws:
     *
     *  - {@link TimeoutException} wrapped in a {@link RuntimeException}: If the timeout is exceeded.
     *  - {@link BackpressureException}: If the incoming request rate is too high to be processed.
     *  - {@link IllegalStateException}: If the view is reduced and the ID is null.
     *  - {@link TranscodingException}: If the response document could not be decoded.
     *
     * @param target the custom target document type.
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return the loaded document, null if not found.
     */
    <D extends Document<?>> D document(final Class<D> target, long timeout, TimeUnit timeUnit);

}
