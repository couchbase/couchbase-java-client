package com.couchbase.client.java.query;

import com.couchbase.client.java.document.json.JsonObject;

public class DefaultQueryRow implements QueryRow {

    private final JsonObject value;

    public DefaultQueryRow(JsonObject value) {
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
