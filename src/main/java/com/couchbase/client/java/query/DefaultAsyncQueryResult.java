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
    private final boolean parsingSuccess;
    private final Observable<JsonObject> errors;
    private final Observable<Boolean> finalSuccess;

    public DefaultAsyncQueryResult(Observable<AsyncQueryRow> rows, Observable<JsonObject> info,
            Observable<JsonObject> errors, Observable<Boolean> finalSuccess, boolean parsingSuccess) {
        this.rows = rows;
        this.info = info;
        this.errors = errors;
        this.finalSuccess = finalSuccess;
        this.parsingSuccess = parsingSuccess;
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
    public Observable<Boolean> finalSuccess() {
        return finalSuccess;
    }

    /**
     * This only denotes initial success in parsing the query. As rows are processed, it could be
     * that a late failure occurs. See {@link #finalSuccess} for the end of processing status.
     *
     * {@inheritDoc}
     *
     * @return true if no errors were detected upfront / query was successfully parsed.
     */
    @Override
    public boolean parseSuccess() {
        return parsingSuccess;
    }

    @Override
    public Observable<JsonObject> errors() {
        return errors;
    }
}
