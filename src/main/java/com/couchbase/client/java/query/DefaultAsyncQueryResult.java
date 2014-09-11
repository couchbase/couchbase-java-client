package com.couchbase.client.java.query;

import com.couchbase.client.java.document.json.JsonObject;
import rx.Observable;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DefaultAsyncQueryResult implements AsyncQueryResult {

    private final Observable<AsyncQueryRow> rows;
    private final Observable<JsonObject> info;
    private final boolean success;
    private final JsonObject error;

    public DefaultAsyncQueryResult(Observable<AsyncQueryRow> rows, Observable<JsonObject> info, JsonObject error, boolean success) {
        this.rows = rows;
        this.info = info;
        this.error = error;
        this.success = success;
    }

    @Override
    public Observable<AsyncQueryRow> rows() {
        return rows;
    }

    @Override
    public Observable<JsonObject> info() {
        return info;
    }

    @Override
    public boolean success() {
        return success;
    }

    @Override
    public JsonObject error() {
        return error;
    }
}
