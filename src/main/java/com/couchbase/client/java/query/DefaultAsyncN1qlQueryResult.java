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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonObject;
import rx.Observable;
import rx.functions.Func1;

/**
 * The default implementation of an {@link AsyncN1qlQueryResult}.
 *
 * @author Michael Nitschinger
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public class DefaultAsyncN1qlQueryResult implements AsyncN1qlQueryResult {

    private final Observable<AsyncN1qlQueryRow> rows;
    private final Observable<Object> signature;
    private final Observable<N1qlMetrics> info;
    private final boolean parsingSuccess;
    private final Observable<JsonObject> errors;
    private final Observable<JsonObject> profileInfo;
    private final Observable<String> finalStatus;
    private final String requestId;
    private final String clientContextId;

    public DefaultAsyncN1qlQueryResult(Observable<AsyncN1qlQueryRow> rows, Observable<Object> signature,
                                       Observable<N1qlMetrics> info, Observable<JsonObject> errors, Observable<JsonObject> profileInfo,
                                       Observable<String> finalStatus, boolean parsingSuccess, String requestId, String clientContextId) {
        this.rows = rows;
        this.signature = signature;
        this.info = info;
        this.errors = errors;
        this.profileInfo = profileInfo;
        this.finalStatus = finalStatus;
        this.parsingSuccess = parsingSuccess;
        this.requestId = requestId;
        this.clientContextId = clientContextId;
    }

    @Override
    public Observable<AsyncN1qlQueryRow> rows() {
        return rows;
    }

    @Override
    public Observable<Object> signature() {
        return signature;
    }

    @Override
    public Observable<N1qlMetrics> info() {
        return info;
    }

    @Override
    public Observable<JsonObject> profileInfo() {
        return profileInfo;
    }

    @Override
    public Observable<Boolean> finalSuccess() {
        return finalStatus.map(new Func1<String, Boolean>() {
            @Override
            public Boolean call(String status) {
                return "success".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status);
            }
        });
    }

    @Override
    public Observable<String> status() {
        return finalStatus;
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
