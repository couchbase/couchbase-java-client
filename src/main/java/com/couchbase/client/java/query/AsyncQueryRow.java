package com.couchbase.client.java.query;

import com.couchbase.client.java.document.json.JsonObject;

public interface AsyncQueryRow {

    JsonObject value();
}
