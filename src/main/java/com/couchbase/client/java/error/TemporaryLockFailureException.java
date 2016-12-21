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
        super();
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
