package com.couchbase.client.java.cluster;

import com.couchbase.client.core.CouchbaseException;

public class BucketExistsException extends CouchbaseException {

    public BucketExistsException() {
    }

    public BucketExistsException(String message) {
        super(message);
    }

    public BucketExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public BucketExistsException(Throwable cause) {
        super(cause);
    }
}
