/*
 * Copyright (C) 2016 Couchbase, Inc.
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

package com.couchbase.client.java.subdoc;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.message.ResponseStatus;

/**
 * Internally represent result corresponding to a single {@link LookupSpec} or {@link MutationSpec},
 * as part of a {@link DocumentFragment}.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Private
public class SubdocOperationResult<OPERATION> {

    private final String path;
    private final OPERATION operation;

    private final ResponseStatus status;

    private final Object value;

    /**
     * Create a new MultiResult.
     *
     * @param path the path that was looked up in a document.
     * @param operation the kind of operation that was performed.
     * @param status the status for this lookup.
     * @param value the value for a successful GET, true/false for a EXIST, an Exception in case of FAILURE, null otherwise.
     */
    private SubdocOperationResult(String path, OPERATION operation, ResponseStatus status, Object value) {
        this.path = path;
        this.operation = operation;
        this.status = status;
        this.value = value;
    }

    /**
     * Create a {@link SubdocOperationResult} that denotes that a fatal Exception occurred when parsing
     * server-side result. The exception can be found as the value, and the status is {@link ResponseStatus#FAILURE}.
     *
     * @param path the path looked up.
     * @param operation the type of the operation which result couldn't be parsed.
     * @param fatal the Exception that occurred during response parsing.
     * @return the fatal LookupResult.
     */
    public static <OPERATION> SubdocOperationResult<OPERATION> createFatal(String path, OPERATION operation, RuntimeException fatal) {
        return new SubdocOperationResult<OPERATION>(path, operation, ResponseStatus.FAILURE, fatal);
    }

    /**
     * Create a {@link SubdocOperationResult} that corresponds to an operation that should return a content (including null)
     * instead of throwing when calling {@link DocumentFragment#content(String)}.
     *
     * @param path the path looked up.
     * @param operation the type of operation.
     * @param status the status of the operation.
     * @param value the value of the operation if applicable, null otherwise.
     * @return the operation result.
     */
    public static <OPERATION> SubdocOperationResult<OPERATION> createResult(String path, OPERATION operation, ResponseStatus status, Object value) {
        return new SubdocOperationResult<OPERATION>(path, operation, status, value);
    }

    /**
     * Create a {@link SubdocOperationResult} that correspond to a subdoc-level error, to be thrown by the enclosing
     * {@link DocumentFragment#content(String) DocumentFragment when calling content methods}.
     *
     * @param path the path looked up.
     * @param operation the type of operation.
     * @param status the status of the operation.
     * @param exception the exception to throw when trying to access the operation content.
     * @return the operation result.
     */
    public static <OPERATION> SubdocOperationResult<OPERATION> createError(String path, OPERATION operation, ResponseStatus status, CouchbaseException exception) {
        return new SubdocOperationResult<OPERATION>(path, operation, status, exception);
    }

    /**
     * @return the path that was looked up.
     */
    public String path() {
        return path;
    }

    /**
     * @return the exact kind of lookup that was performed.
     */
    public OPERATION operation() {
        return operation;
    }

    /**
     * @return the status of the lookup.
     */
    public ResponseStatus status() {
        return status;
    }

    /**
     * @return true if the value existed (and might have associated value), false otherwise.
     */
    public boolean exists() {
        return status.isSuccess();
    }

    /**
     * Returns:
     *  - the value retrieved by a successful GET.
     *  - null for an unsuccessful GET (see the {@link #status()} for details).
     *  - true/false for an EXIST (equivalent to {@link #exists()}).
     *  - a {@link RuntimeException} if the client side parsing of the result failed ({@link #isFatal()}).
     *
     * @return the value
     */
    public Object value() {
        return value;
    }

    /**
     * @return true if there was a fatal error while processing the result on the client side, in which case
     * {@link #value()} returns an {@link RuntimeException}.
     */
    public boolean isFatal() {
        return status == ResponseStatus.FAILURE;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(operation).append('(').append(path).append("){");
        if (status.isSuccess() || !(value instanceof Exception)) {
            sb.append("value=").append(value);
        } else if (status == ResponseStatus.FAILURE) {
            sb.append("fatal=").append(value);
        } else {
            sb.append("error=").append(status);
        }
        return sb.append('}').toString();
    }
}
