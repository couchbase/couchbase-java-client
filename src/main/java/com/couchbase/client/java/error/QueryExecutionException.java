/**
 * Copyright (C) 2015 Couchbase, Inc.
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
package com.couchbase.client.java.error;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.java.document.json.JsonObject;

/**
 * A {@link CouchbaseException} representing various errors during N1QL querying, when an
 * actual Exception wrapping a {@link JsonObject} is needed.
 *
 * @author Simon Baslé
 * @since 2.2
 */
public class QueryExecutionException extends CouchbaseException {

    private final JsonObject n1qlError;

    public QueryExecutionException(String message, JsonObject n1qlError) {
        super(message);
        this.n1qlError = n1qlError == null ? JsonObject.empty() : n1qlError;
    }

    public QueryExecutionException(String message, JsonObject n1qlError, Throwable cause) {
        super(message, cause);
        this.n1qlError = n1qlError == null ? JsonObject.empty() : n1qlError;
    }

    public JsonObject getN1qlError() {
        return this.n1qlError;
    }
}
