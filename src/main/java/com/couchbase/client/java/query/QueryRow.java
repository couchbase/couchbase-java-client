package com.couchbase.client.java.query;

import com.couchbase.client.java.document.json.JsonObject;

public interface QueryRow {

    JsonObject value();
}
