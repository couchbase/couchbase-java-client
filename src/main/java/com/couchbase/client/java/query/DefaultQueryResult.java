package com.couchbase.client.java.query;

import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.util.Blocking;
import rx.Observable;
import rx.functions.Func1;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DefaultQueryResult implements QueryResult {

    private boolean finalSuccess;
    private boolean parseSuccess;
    private List<QueryRow> allRows;
    private JsonObject info;
    private List<JsonObject> errors;


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
            long timeout, TimeUnit timeUnit) {

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
}
