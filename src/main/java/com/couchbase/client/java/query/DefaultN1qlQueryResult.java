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
package com.couchbase.client.java.query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonObject;

@InterfaceStability.Committed
@InterfaceAudience.Public
public class DefaultN1qlQueryResult implements N1qlQueryResult {

    private final String status;
    private final boolean finalSuccess;
    private final boolean parseSuccess;
    private final List<N1qlQueryRow> allRows;
    private final Object signature;
    private final N1qlMetrics info;
    private final JsonObject profileInfo;
    private final List<JsonObject> errors;
    private final String requestId;
    private final String clientContextId;


    /**
     * Create a default blocking representation of a query result.
     *
     * @param rows the list of rows.
     * @param signature the signature for rows.
     * @param info the metrics.
     * @param profileInfo the profile information
     * @param errors the list of errors and warnings.
     * @param finalStatus the definitive (but potentially delayed) status of the query.
     * @param finalSuccess the definitive (but potentially delayed) success of the query.
     * @param parseSuccess the intermediate result of the query
     */
    public DefaultN1qlQueryResult(List<AsyncN1qlQueryRow> rows, Object signature,
                                  N1qlMetrics info, List<JsonObject> errors, JsonObject profileInfo,
                                  String finalStatus, Boolean finalSuccess, boolean parseSuccess,
                                  String requestId, String clientContextId) {

        this.requestId = requestId;
        this.clientContextId = clientContextId;
        this.parseSuccess = parseSuccess;
        this.finalSuccess = finalSuccess != null && finalSuccess;
        this.status = finalStatus;
        this.allRows = new ArrayList<N1qlQueryRow>(rows.size());
        for (AsyncN1qlQueryRow row : rows) {
            this.allRows.add(new DefaultN1qlQueryRow(row));
        }
        this.signature = signature;
        this.errors = errors;
        this.info = info;
        this.profileInfo = profileInfo;
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
    public JsonObject profileInfo() {
        return this.profileInfo;
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
        return status;
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

    @Override
    public String toString() {
        return "N1qlQueryResult{" +
                "status='" + status + '\'' +
                ", finalSuccess=" + finalSuccess +
                ", parseSuccess=" + parseSuccess +
                ", allRows=" + allRows +
                ", signature=" + signature +
                ", info=" + info +
                ", profileInfo=" + profileInfo +
                ", errors=" + errors +
                ", requestId='" + requestId + '\'' +
                ", clientContextId='" + clientContextId + '\'' +
                '}';
    }
}
