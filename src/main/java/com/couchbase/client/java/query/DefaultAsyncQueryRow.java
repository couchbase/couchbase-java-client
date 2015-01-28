package com.couchbase.client.java.query;

import com.couchbase.client.java.document.json.JsonObject;

public class DefaultAsyncQueryRow implements AsyncQueryRow {

    private final JsonObject value;

    public DefaultAsyncQueryRow(JsonObject value) {
        this.value = value;
    }

    public JsonObject value() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
