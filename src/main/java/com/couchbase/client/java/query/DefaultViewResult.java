package com.couchbase.client.java.query;

import com.couchbase.client.java.document.json.JsonObject;
import rx.Observable;

/**
 * Created by michael on 05/05/14.
 */
public class DefaultViewResult implements ViewResult {

    private final Observable<ViewRow> rows;
    private final int totalRows;
    private final boolean success;
    private final JsonObject error;
    private final JsonObject debug;

    public DefaultViewResult(Observable<ViewRow> rows, int totalRows, boolean success, JsonObject error, JsonObject debug) {
        this.rows = rows;
        this.totalRows = totalRows;
        this.success = success;
        this.error = error;
        this.debug = debug;
    }

    @Override
    public Observable<ViewRow> rows() {
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
