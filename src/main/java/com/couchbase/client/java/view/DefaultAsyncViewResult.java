package com.couchbase.client.java.view;

import com.couchbase.client.java.document.json.JsonObject;
import rx.Observable;

/**
 * Created by michael on 05/05/14.
 */
public class DefaultAsyncViewResult implements AsyncViewResult {

    private final Observable<AsyncViewRow> rows;
    private final int totalRows;
    private final boolean success;
    private final JsonObject error;
    private final JsonObject debug;

    public DefaultAsyncViewResult(Observable<AsyncViewRow> rows, int totalRows, boolean success, JsonObject error, JsonObject debug) {
        this.rows = rows;
        this.totalRows = totalRows;
        this.success = success;
        this.error = error;
        this.debug = debug;
    }

    @Override
    public Observable<AsyncViewRow> rows() {
        return rows;
    }

    @Override
    public int totalRows() {
        return totalRows;
    }

    @Override
    public boolean success() {
        return success;
    }

    @Override
    public JsonObject error() {
        return error;
    }

    @Override
    public JsonObject debug() {
        return debug;
    }
}
