package com.couchbase.client.java.query;

import com.couchbase.client.java.document.json.JsonObject;

/**
 * Created by michael on 21/05/14.
 */
public class QueryResult {

    private final JsonObject value;

    public QueryResult(JsonObject value) {
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
