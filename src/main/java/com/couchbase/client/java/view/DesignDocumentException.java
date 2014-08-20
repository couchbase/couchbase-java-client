package com.couchbase.client.java.view;

import com.couchbase.client.core.CouchbaseException;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DesignDocumentException extends CouchbaseException {

    public DesignDocumentException() {
    }

    public DesignDocumentException(String message) {
        super(message);
    }

    public DesignDocumentException(String message, Throwable cause) {
        super(message, cause);
    }

    public DesignDocumentException(Throwable cause) {
        super(cause);
    }
}
