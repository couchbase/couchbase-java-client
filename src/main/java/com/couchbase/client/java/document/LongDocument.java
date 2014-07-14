package com.couchbase.client.java.document;

import com.couchbase.client.core.message.ResponseStatus;

public class LongDocument extends AbstractDocument<Long> {

    public LongDocument(String id, Long content, long cas, int expiry, ResponseStatus status) {
        super(id, content, cas, expiry, status);
    }
}
