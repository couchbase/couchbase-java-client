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
 * Represents a {@link ViewRow} fetched from the View.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface ViewRow {

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
     * Load the underlying document, if not reduced with the default view timeout.
     *
     * This method throws:
     *
     *  - {@link TimeoutException}: If the timeout is exceeded.
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
     *  - {@link TimeoutException}: If the timeout is exceeded.
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
     *  - {@link TimeoutException}: If the timeout is exceeded.
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
     *  - {@link TimeoutException}: If the timeout is exceeded.
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
