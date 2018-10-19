/*
 * Copyright (c) 2017 Couchbase, Inc.
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
import rx.Observable;
import rx.functions.Func1;

@InterfaceStability.Committed
@InterfaceAudience.Public
public class DefaultAsyncAnalyticsQueryResult implements AsyncAnalyticsQueryResult {

    private Observable<AsyncAnalyticsQueryRow> rows;
    private final Observable<Object> signature;
    private final Observable<AnalyticsMetrics> info;
    private final boolean parsingSuccess;
    private final Observable<JsonObject> errors;
    private Observable<String> finalStatus;
    private final String requestId;
    private final String clientContextId;
    private AsyncAnalyticsDeferredResultHandle handle;

    public DefaultAsyncAnalyticsQueryResult(Observable<AsyncAnalyticsQueryRow> rows, Observable<Object> signature,
        Observable<AnalyticsMetrics> info, Observable<JsonObject> errors, Observable<String> finalStatus,
        boolean parsingSuccess, String requestId, String clientContextId) {
        this.rows = rows;
        this.signature = signature;
        this.info = info;
        this.errors = errors;
        this.finalStatus = finalStatus;
        this.parsingSuccess = parsingSuccess;
        this.requestId = requestId;
        this.clientContextId = clientContextId;
    }

    public DefaultAsyncAnalyticsQueryResult(AsyncAnalyticsDeferredResultHandle handle, Observable<Object> signature,
                                            Observable<AnalyticsMetrics> info, Observable<JsonObject> errors,
                                            Observable<String> finalStatus, boolean parsingSuccess, String requestId,
                                            String clientContextId) {
        this.handle = handle;
        this.signature = signature;
        this.info = info;
        this.errors = errors;
        this.finalStatus = finalStatus;
        this.parsingSuccess = parsingSuccess;
        this.requestId = requestId;
        this.clientContextId = clientContextId;
        this.rows = Observable.empty();
    }

    @Override
    public Observable<AsyncAnalyticsQueryRow> rows() {
        return finalStatus.flatMap(new Func1<String, Observable<AsyncAnalyticsQueryRow>>() {
            @Override
            public Observable<AsyncAnalyticsQueryRow> call(String s) {
                if (s.equalsIgnoreCase("running")) {
                    return Observable.just(null);
                } else {
                    return rows;
                }
            }
        });
    }

    @Override
    public Observable<Object> signature() {
        return signature;
    }

    @Override
    public Observable<AnalyticsMetrics> info() {
        return info;
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

    @Override
    public AsyncAnalyticsDeferredResultHandle handle() { return handle; }
}