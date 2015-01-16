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

    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
    private final DefaultAsyncQueryResult asyncQueryResult;
    private final long timeout;

    public DefaultQueryResult(CouchbaseEnvironment environment, Observable<AsyncQueryRow> rows,
            Observable<JsonObject> info, Observable<JsonObject> errors,
            Observable<Boolean> finalSuccess, boolean parseSuccess) {
        this.asyncQueryResult = new DefaultAsyncQueryResult(rows, info, errors, finalSuccess, parseSuccess);
        this.timeout = environment.managementTimeout();
    }

    @Override
    public List<QueryRow> allRows() {
        return allRows(timeout, TIMEOUT_UNIT);
    }

    @Override
    public List<QueryRow> allRows(long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncQueryResult
            .rows()
            .map(new Func1<AsyncQueryRow, QueryRow>() {
                @Override
                public QueryRow call(AsyncQueryRow asyncQueryRow) {
                    return new DefaultQueryRow(asyncQueryRow.value());
                }
            })
            .toList(), timeout, timeUnit);
    }

    @Override
    public Iterator<QueryRow> rows() {
        return rows(timeout, TIMEOUT_UNIT);
    }

    @Override
    public Iterator<QueryRow> rows(long timeout, TimeUnit timeUnit) {
        return asyncQueryResult
            .rows()
            .map(new Func1<AsyncQueryRow, QueryRow>() {
                @Override
                public QueryRow call(AsyncQueryRow asyncQueryRow) {
                    return new DefaultQueryRow(asyncQueryRow.value());
                }
            })
            .timeout(timeout, timeUnit)
            .toBlocking()
            .getIterator();
    }

    @Override
    public JsonObject info() {
        return info(timeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonObject info(long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncQueryResult.info().single(), timeout, timeUnit);
    }

    @Override
    public boolean parseSuccess() {
        return asyncQueryResult.parseSuccess();
    }

    @Override
    public List<JsonObject> errors() {
        return Blocking.blockForSingle(
                asyncQueryResult.errors().toList(), timeout, TIMEOUT_UNIT);
    }

    @Override
    public boolean finalSuccess() {
        return Blocking.blockForSingle(asyncQueryResult.finalSuccess().single(), timeout, TIMEOUT_UNIT);
    }

    @Override
    public Iterator<QueryRow> iterator() {
        return rows();
    }
}
