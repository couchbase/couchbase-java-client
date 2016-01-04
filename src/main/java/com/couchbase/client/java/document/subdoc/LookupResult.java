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

package com.couchbase.client.java.document.subdoc;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.subdoc.multi.Lookup;

/**
 * A result corresponding to a single {@link LookupSpec}, usually grouped inside a {@link MultiLookupResult}.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class LookupResult {

    private final String path;
    private final Lookup operation;

    private final ResponseStatus status;

    private final Object value;

    /**
     * Create a new LookupResult.
     *
     * @param path the path that was looked up in a document.
     * @param operation the kind of lookup that was performed.
     * @param status the status for this lookup.
     * @param value the value for a successful GET, true/false for a EXIST, an Exception in case of FAILURE, null otherwise.
     */
    private LookupResult(String path, Lookup operation, ResponseStatus status, Object value) {
        this.path = path;
        this.operation = operation;
        this.status = status;
        this.value = value;
    }

    /**
     * Create a {@link LookupResult} that denotes that a fatal Exception occurred when parsing
     * server-side result. The exception can be found as the value, and the status is {@link ResponseStatus#FAILURE}.
     *
     * @param path the path looked up.
     * @param operation the lookup operation which result couldn't be parsed.
     * @param fatal the Exception that occurred during response parsing.
     * @return the fatal LookupResult.
     */
    public static LookupResult createFatal(String path, Lookup operation, RuntimeException fatal) {
        return new LookupResult(path, operation, ResponseStatus.FAILURE, fatal);
    }

    /**
     * Create a {@link LookupResult} that corresponds to a GET.
     *
     * @param path the path looked up.
     * @param status the status of the GET.
     * @param value the value of the GET if successful, null otherwise.
     * @return the GET LookupResult.
     */
    public static LookupResult createGetResult(String path, ResponseStatus status, Object value) {
        return new LookupResult(path, Lookup.GET, status, value);
    }

    /**
     * Create a {@link LookupResult} that corresponds to a EXIST.
     *
     * @param path the path looked up.
     * @param status the status of the EXIST, its {@link ResponseStatus#isSuccess() isSuccess}
     *               giving the LookupResult's value.
     * @return the EXIST LookupResult.
     */
    public static LookupResult createExistResult(String path, ResponseStatus status) {
        return new LookupResult(path, Lookup.EXIST, status, status.isSuccess());
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
    public Lookup operation() {
        return operation;
    }

    /**
     * @return the status of the lookup.
     */
    public ResponseStatus status() {
        return status;
    }

    /**
     * @return true if the value existed (and in the case of a GET, could be retrieved), false otherwise.
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
     * @see #valueOrThrow() for a version that throws the exception instead of returning it.
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

    /**
     * Returns:
     *  - the value retrieved by a successful GET.
     *  - null for an unsuccessful GET (see the {@link #status()} for details).
     *  - true/false for an EXIST (equivalent to {@link #exists()}).
     *
     * Throws:
     *  - a {@link RuntimeException} if the client side parsing of the result failed ({@link #isFatal()}).
     *
     * @return the value
     * @see #value() for a version that just returns the exception instead of throwing it.
     */
    public Object valueOrThrow() {
        if (isFatal()) {
            throw (RuntimeException) value;
        }
        return value;
    }
}
