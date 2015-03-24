package com.couchbase.client.java.query;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonObject;
import rx.Observable;
import rx.functions.Func1;

/**
 * The default implementation of an {@link AsyncQueryResult}.
 *
 * @author Michael Nitschinger
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Public
public class DefaultAsyncQueryResult implements AsyncQueryResult {

    private final Observable<AsyncQueryRow> rows;
    private final Observable<Object> signature;
    private final Observable<QueryMetrics> info;
    private final boolean parsingSuccess;
    private final Observable<JsonObject> errors;
    private final Observable<Boolean> finalSuccess;
    private final String requestId;
    private final String clientContextId;

    public DefaultAsyncQueryResult(Observable<AsyncQueryRow> rows, Observable<Object> signature,
            Observable<JsonObject> info, Observable<JsonObject> errors, Observable<Boolean> finalSuccess,
            boolean parsingSuccess, String requestId, String clientContextId) {
        this.rows = rows;
        this.signature = signature;
        this.info = info.map(new Func1<JsonObject, QueryMetrics>() {
            @Override
            public QueryMetrics call(JsonObject jsonObject) {
                return new QueryMetrics(jsonObject);
            }
        });
        this.errors = errors;
        this.finalSuccess = finalSuccess;
        this.parsingSuccess = parsingSuccess;
        this.requestId = requestId;
        this.clientContextId = clientContextId;
    }

    @Override
    public Observable<AsyncQueryRow> rows() {
        return rows;
    }

    @Override
    public Observable<Object> signature() {
        return signature;
    }

    @Override
    public Observable<QueryMetrics> info() {
        return info;
    }

    @Override
    public Observable<Boolean> finalSuccess() {
        return finalSuccess;
    }

    @Override
    public boolean parseSuccess() {
        return parsingSuccess;
    }

    @Override
    public Observable<JsonObject> errors() {
        return errors;
    }

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    public String clientContextId() {
        return clientContextId;
    }
}
