package com.couchbase.client.java.cluster;

import com.couchbase.client.core.CouchbaseException;


public class BucketDoesNotExistException extends CouchbaseException {

    public BucketDoesNotExistException() {
    }

    public BucketDoesNotExistException(String message) {
        super(message);
    }

    public BucketDoesNotExistException(String message, Throwable cause) {
        super(message, cause);
    }

    public BucketDoesNotExistException(Throwable cause) {
        super(cause);
    }
}
