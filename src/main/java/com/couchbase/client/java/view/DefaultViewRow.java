package com.couchbase.client.java.view;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.env.CouchbaseEnvironment;

import java.util.concurrent.TimeUnit;

public class DefaultViewRow implements ViewRow {

    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
    private final AsyncViewRow asyncViewRow;
    private final long timeout;

    public DefaultViewRow(CouchbaseEnvironment env, Bucket bucket, String id, Object key, Object value) {
        this.asyncViewRow = new DefaultAsyncViewRow(bucket.async(), id, key, value);
        this.timeout = env.kvTimeout();
    }

    @Override
    public String id() {
        return asyncViewRow.id();
    }

    @Override
    public Object key() {
        return asyncViewRow.key();
    }

    @Override
    public Object value() {
        return asyncViewRow.value();
    }

    @Override
    public JsonDocument document() {
        return document(timeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonDocument document(long timeout, TimeUnit timeUnit) {
        return asyncViewRow
            .document()
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public <D extends Document<?>> D document(Class<D> target) {
        return document(target, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D document(Class<D> target, long timeout, TimeUnit timeUnit) {
        return asyncViewRow
            .document(target)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DefaultViewRow{");
        sb.append("id=").append(id());
        sb.append(", key=").append(key());
        sb.append(", value=").append(value());
        sb.append('}');
        return sb.toString();
    }
}
