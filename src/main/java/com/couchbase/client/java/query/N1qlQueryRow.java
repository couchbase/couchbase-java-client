package com.couchbase.client.java.query;

import com.couchbase.client.java.document.json.JsonObject;

public interface N1qlQueryRow {

    JsonObject value();
}
