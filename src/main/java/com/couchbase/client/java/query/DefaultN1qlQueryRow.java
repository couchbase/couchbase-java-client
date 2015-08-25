package com.couchbase.client.java.query;

import com.couchbase.client.java.document.json.JsonObject;

public class DefaultN1qlQueryRow implements N1qlQueryRow {

    private final JsonObject value;

    public DefaultN1qlQueryRow(JsonObject value) {
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
