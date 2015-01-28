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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonObject;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Represents the result from a {@link ViewQuery}.
 *
 * @author Michael Nitschinger
 * @since 2.1.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface SpatialViewResult extends Iterable<SpatialViewRow> {

    /**
     * Collects all rows received from the view with the default view timeout.
     *
     * This method throws:
     *
     * - {@link TimeoutException} wrapped in a {@link RuntimeException}: If the timeout is exceeded.
     *
     * @return a (potentially empty) {@link List} containing view rows.
     */
    List<SpatialViewRow> allRows();

    /**
     * Collects all rows received from the view with the default view timeout.
     *
     * This method throws:
     *
     * - {@link TimeoutException} wrapped in a {@link RuntimeException}: If the timeout is exceeded.
     *
     * @return a (potentially empty) {@link List} containing view rows.
     */
    List<SpatialViewRow> allRows(long timeout, TimeUnit timeUnit);

    /**
     * Emits one {@link ViewRow} for each row received from the view with the default view timeout.
     *
     * This method throws:
     *
     * - {@link TimeoutException} wrapped in a {@link RuntimeException}: If the timeout is exceeded.
     *
     * @return a (potentially empty) {@link Iterator} containing view rows.
     */
    Iterator<SpatialViewRow> rows();

    /**
     * Emits one {@link ViewRow} for each row received from the view with a custom timeout.
     *
     * This method throws:
     *
     * - {@link TimeoutException} wrapped in a {@link RuntimeException}: If the timeout is exceeded.
     *
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return a (potentially empty) {@link Iterator} containing view rows.
     */
    Iterator<SpatialViewRow> rows(long timeout, TimeUnit timeUnit);

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
    JsonObject error();

    /**
     * If debug was enabled on the query, it is contained here.
     *
     * @return the debug info.
     */
    JsonObject debug();
}
