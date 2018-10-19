/*
 * Copyright (c) 2016 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.client.java.analytics;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@InterfaceStability.Committed
@InterfaceAudience.Public
public class DefaultAnalyticsQueryResult implements AnalyticsQueryResult {

    private String status;
    private final boolean finalSuccess;
    private final boolean parseSuccess;
    private List<AnalyticsQueryRow> allRows;
    private final Object signature;
    private final AnalyticsMetrics info;
    private final List<JsonObject> errors;
    private final String requestId;
    private final String clientContextId;
    private final AsyncAnalyticsDeferredResultHandle asyncHandle;
    private final AnalyticsDeferredResultHandle handle;

    /**
     * Create a default blocking representation of a query result.
     *
     * @param rows the list of rows.
     * @param signature the signature for rows.
     * @param info the metrics.
     * @param errors the list of errors and warnings.
     * @param finalStatus the definitive (but potentially delayed) status of the query.
     * @param finalSuccess the definitive (but potentially delayed) success of the query.
     * @param parseSuccess the intermediate result of the query
     */
    public DefaultAnalyticsQueryResult(List<AsyncAnalyticsQueryRow> rows, Object signature,
        AnalyticsMetrics info, List<JsonObject> errors,
        String finalStatus, Boolean finalSuccess, boolean parseSuccess,
        String requestId, String clientContextId) {

        this.requestId = requestId;
        this.clientContextId = clientContextId;
        this.parseSuccess = parseSuccess;
        this.finalSuccess = finalSuccess != null && finalSuccess;
        this.status = finalStatus;
        this.allRows = new ArrayList<AnalyticsQueryRow>(rows.size());
        for (AsyncAnalyticsQueryRow row : rows) {
            this.allRows.add(new DefaultAnalyticsQueryRow(row));
        }
        this.signature = signature;
        this.errors = errors;
        this.info = info;
        this.handle = null;
        this.asyncHandle = null;
    }

    /**
     * Create a default blocking representation of a query result.
     *
     * @param asyncHandle the deferred result handle.
     * @param signature the signature for rows.
     * @param info the metrics.
     * @param errors the list of errors and warnings.
     * @param finalStatus the definitive (but potentially delayed) status of the query.
     * @param finalSuccess the definitive (but potentially delayed) success of the query.
     * @param parseSuccess the intermediate result of the query
     */
    public DefaultAnalyticsQueryResult(AsyncAnalyticsDeferredResultHandle asyncHandle, Object signature,
                                       AnalyticsMetrics info, List<JsonObject> errors,
                                       String finalStatus, Boolean finalSuccess, boolean parseSuccess,
                                       String requestId, String clientContextId) {

        this.asyncHandle = asyncHandle;
        this.handle = new DefaultAnalyticsDeferredResultHandle(this.asyncHandle);
        this.requestId = requestId;
        this.clientContextId = clientContextId;
        this.parseSuccess = parseSuccess;
        this.finalSuccess = finalSuccess != null && finalSuccess;
        this.status = finalStatus;
        this.signature = signature;
        this.errors = errors;
        this.info = info;
    }

    @Override
    public List<AnalyticsQueryRow> allRows() {
        if (this.status.equalsIgnoreCase("running")) {
            return null;
        }
        return this.allRows;
    }

    @Override
    public Iterator<AnalyticsQueryRow> rows() {
        if (this.status.equalsIgnoreCase("running")) {
            return null;
        }
        return this.allRows().iterator();
    }

    @Override
    public Object signature() {
        return this.signature;
    }

    @Override
    public AnalyticsMetrics info() {
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
    public String status() {
        return this.status;
    }

    @Override
    public Iterator<AnalyticsQueryRow> iterator() {
        return this.rows();
    }

    @Override
    public String requestId() {
        return this.requestId;
    }

    @Override
    public String clientContextId() {
        return this.clientContextId;
    }

    @Override
    public AnalyticsDeferredResultHandle handle() {
        return this.handle;
    }

    @Override
    public String toString() {
        return "DefaultAnalyticsQueryResult{" +
            "status='" + status + '\'' +
            ", finalSuccess=" + finalSuccess +
            ", parseSuccess=" + parseSuccess +
            ", allRows=" + allRows +
            ", signature=" + signature +
            ", info=" + info +
            ", errors=" + errors +
            ", requestId='" + requestId + '\'' +
            ", clientContextId='" + clientContextId + '\'' +
            ", handle='" + handle+ '\'' +
            '}';
    }
}
