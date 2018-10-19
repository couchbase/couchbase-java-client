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
package com.couchbase.client.java.error;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.java.document.json.JsonObject;

/**
 * A {@link CouchbaseException} representing various errors during N1QL querying, when an
 * actual Exception wrapping a {@link JsonObject} is needed.
 * It can also be thrown when the result handle is not available during deferred query rows
 * fetch, the result handle is available only when the status poll returns success.
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
