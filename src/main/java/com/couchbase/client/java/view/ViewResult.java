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

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Represents the result from a {@link ViewQuery}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface ViewResult extends Iterable<ViewRow> {

    /**
     * Collects all rows received from the view with the default view timeout.
     *
     * This method throws:
     *
     * - {@link TimeoutException} wrapped in a {@link RuntimeException}: If the timeout is exceeded.
     *
     * @return a (potentially empty) {@link List} containing view rows.
     */
    List<ViewRow> allRows();

    /**
     * Collects all rows received from the view with the default view timeout.
     *
     * This method throws:
     *
     * - {@link TimeoutException} wrapped in a {@link RuntimeException}: If the timeout is exceeded.
     *
     * @return a (potentially empty) {@link List} containing view rows.
     */
    List<ViewRow> allRows(long timeout, TimeUnit timeUnit);

    /**
     * Emits one {@link ViewRow} for each row received from the view with the default view timeout.
     *
     * This method throws:
     *
     * - {@link TimeoutException} wrapped in a {@link RuntimeException}: If the timeout is exceeded.
     *
     * @return a (potentially empty) {@link Iterator} containing view rows.
     */
    Iterator<ViewRow> rows();

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
    Iterator<ViewRow> rows(long timeout, TimeUnit timeUnit);

    /**
     * The total number of rows.
     *
     * @return number of rows.
     */
    int totalRows();

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
     * If it was not successful, an error is contained here.
     *
     * @return the potential error.
     */
    JsonObject error(long timeout, TimeUnit timeUnit);

    /**
     * If debug was enabled on the query, it is contained here.
     *
     * @return the debug info.
     */
    JsonObject debug();
}
