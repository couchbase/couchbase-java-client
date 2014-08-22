package com.couchbase.client.java.error;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DesignDocumentAlreadyExistsException extends DesignDocumentException {

    public DesignDocumentAlreadyExistsException() {
    }

    public DesignDocumentAlreadyExistsException(String message) {
        super(message);
    }

    public DesignDocumentAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public DesignDocumentAlreadyExistsException(Throwable cause) {
        super(cause);
    }
}
