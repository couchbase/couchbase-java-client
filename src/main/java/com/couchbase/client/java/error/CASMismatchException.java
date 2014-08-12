package com.couchbase.client.java.error;

import com.couchbase.client.core.CouchbaseException;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class CASMismatchException extends CouchbaseException {

    public CASMismatchException() {
    }

    public CASMismatchException(String message) {
        super(message);
    }

    public CASMismatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public CASMismatchException(Throwable cause) {
        super(cause);
    }
}
