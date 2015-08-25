package com.couchbase.client.java.query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.couchbase.client.java.document.json.JsonObject;

public class DefaultN1qlQueryResult implements N1qlQueryResult {

    private final boolean finalSuccess;
    private final boolean parseSuccess;
    private final List<N1qlQueryRow> allRows;
    private final Object signature;
    private final N1qlMetrics info;
    private final List<JsonObject> errors;
    private final String requestId;
    private final String clientContextId;


    /**
     * Create a default blocking representation of a query result.
     *
     * @param rows the list of rows.
     * @param signature the signature for rows.
     * @param info the metrics.
     * @param errors the list of errors and warnings.
     * @param finalSuccess the definitive (but potentially delayed) result of the query.
     * @param parseSuccess the intermediate result of the query
     */
    public DefaultN1qlQueryResult(List<AsyncN1qlQueryRow> rows, Object signature,
            N1qlMetrics info, List<JsonObject> errors,
            Boolean finalSuccess, boolean parseSuccess,
            String requestId, String clientContextId) {

        this.requestId = requestId;
        this.clientContextId = clientContextId;
        this.parseSuccess = parseSuccess;
        this.finalSuccess = finalSuccess != null && finalSuccess;
        this.allRows = new ArrayList<N1qlQueryRow>(rows.size());
        for (AsyncN1qlQueryRow row : rows) {
            this.allRows.add(new DefaultN1qlQueryRow(row.value()));
        }
        this.signature = signature;
        this.errors = errors;
        this.info = info;
    }

    @Override
    public List<N1qlQueryRow> allRows() {
        return this.allRows;
    }

    @Override
    public Iterator<N1qlQueryRow> rows() {
        return this.allRows.iterator();
    }

    @Override
    public Object signature() {
        return this.signature;
    }

    @Override
    public N1qlMetrics info() {
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
    public Iterator<N1qlQueryRow> iterator() {
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
