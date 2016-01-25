package com.couchbase.client.java.query;

import com.couchbase.client.java.document.json.JsonObject;

public interface AsyncN1qlQueryRow {

    /**
     * @return the raw array of bytes representing the JSON of this row.
     */
    byte[] byteValue();

    /**
     * @return the {@link JsonObject} representation of the JSON corresponding to this row.
     */
    JsonObject value();
}
