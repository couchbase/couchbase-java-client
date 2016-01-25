package com.couchbase.client.java.query;

import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.TranscodingException;

public class DefaultN1qlQueryRow implements N1qlQueryRow {


    private final AsyncN1qlQueryRow asyncRow;

    public DefaultN1qlQueryRow(AsyncN1qlQueryRow asyncRow) {
        this.asyncRow = asyncRow;
    }

    @Override
    public byte[] byteValue() {
        return asyncRow.byteValue();
    }

    /**
     * Return the {@link JsonObject} representation of the JSON corresponding to this row.
     * The {@link JsonObject} is lazily created from {@link #byteValue()} the first time it is requested.
     *
     * @return the JsonObject representation of the value.
     * @throws TranscodingException if the lazy deserialization couldn't be performed due to a Jackson error.
     */
    @Override
    public JsonObject value() {
        return asyncRow.value();
    }

    @Override
    public String toString() {
        return value().toString();
    }
}
