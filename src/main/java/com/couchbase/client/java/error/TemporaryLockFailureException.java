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

/**
 * Thrown when the server reports a temporary failure and it
 * is very likely to be lock-related (like an already locked
 * key or a bad cas used for unlock).
 *
 * This is exception is very likely retryable.
 *
 * See <a href="https://issues.couchbase.com/browse/MB-13087">this issue</a>
 * for a explanation of why this is only likely to be lock-related.
 *
 * @author Simon Baslé
 * @since 2.1.1
 */
public class TemporaryLockFailureException extends CouchbaseException {

    public TemporaryLockFailureException() {
    }

    public TemporaryLockFailureException(String message) {
        super(message);
    }

    public TemporaryLockFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public TemporaryLockFailureException(Throwable cause) {
        super(cause);
    }
}
