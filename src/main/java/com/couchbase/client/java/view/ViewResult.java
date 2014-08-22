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
 * Represents a view result loaded from the index.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public interface ViewResult {

    /**
     * All the rows emitted.
     *
     * @return an observable containing view rows.
     */
    Observable<ViewRow> rows();

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
     * If debug was enabled on the query, it is contained here.
     *
     * @return the debug info.
     */
    JsonObject debug();
}
