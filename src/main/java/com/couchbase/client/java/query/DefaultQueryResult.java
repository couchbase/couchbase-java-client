package com.couchbase.client.java.query;

import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.util.Blocking;
import rx.Observable;
import rx.functions.Func1;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DefaultQueryResult implements QueryResult {

    private final boolean finalSuccess;
    private final boolean parseSuccess;
    private final List<QueryRow> allRows;
    private final JsonObject info;
    private final List<JsonObject> errors;
    private final String requestId;
    private final String clientContextId;


    /**
     * Create a default blocking representation of a query result.
     *
     * @param rows the async view of rows.
     * @param info the async view of metrics.
     * @param errors the async view of errors and warnings.
     * @param finalSuccess the definitive (but potentially delayed) result of the query.
     * @param parseSuccess the intermediate result of the query
     * @param timeout the maximum time allowed for all components of the result to be retrieved (global timeout).
     * @param timeUnit the unit for timeout.
     */
    public DefaultQueryResult(Observable<AsyncQueryRow> rows,
            Observable<JsonObject> info, Observable<JsonObject> errors,
            Observable<Boolean> finalSuccess, boolean parseSuccess,
            String requestId, String clientContextId,
            long timeout, TimeUnit timeUnit) {

        this.requestId = requestId;
        this.clientContextId = clientContextId;
        this.parseSuccess = parseSuccess;
        //block on the finalSuccess item, ensuring streamed section of the result is finished
        this.finalSuccess = Blocking.blockForSingle(finalSuccess, timeout, timeUnit);

        //since we have the final status, other streams should be instantaneous
        this.allRows = Blocking.blockForSingle(rows
                .map(new Func1<AsyncQueryRow, QueryRow>() {
                    @Override
                    public QueryRow call(AsyncQueryRow asyncQueryRow) {
                        return new DefaultQueryRow(asyncQueryRow.value());
                    }
                })
                .toList(), 1, TimeUnit.SECONDS);

        this.errors = Blocking.blockForSingle(errors.toList(), 1, TimeUnit.SECONDS);
        this.info = Blocking.blockForSingle(info.singleOrDefault(JsonObject.empty()), 1, TimeUnit.SECONDS);
    }

    @Override
    public List<QueryRow> allRows() {
        return this.allRows;
    }

    @Override
    public Iterator<QueryRow> rows() {
        return this.allRows.iterator();
    }

    @Override
    public JsonObject info() {
        return this.info;
    }

    @Override
    public boolean parseSuccess() {
        return this.parseSuccess;
    }

    @Override
    public List<JsonObject> errors() {
        return this.errors;
    }

    @Override
    public boolean finalSuccess() {
        return this.finalSuccess;
    }

    @Override
    public Iterator<QueryRow> iterator() {
        return rows();
    }

    @Override
    public String requestId() {
        return this.requestId;
    }

    @Override
    public String clientContextId() {
        return this.clientContextId;
    }
}
