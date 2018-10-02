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

/**
 * The simplest form of Analytics {@link AnalyticsQuery} with a plain un-parameterized Statements.
 *
 * @author Michael Nitschinger
 * @since 2.4.3
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public class SimpleAnalyticsQuery extends AnalyticsQuery {

    private final String statement;
    private final AnalyticsParams params;

    SimpleAnalyticsQuery(String statement, AnalyticsParams params) {
        this.statement = statement.trim();
        this.params = params == null ? AnalyticsParams.build() : params;
    }

    @Override
    public String statement() {
        return statement;
    }

    @Override
    public AnalyticsParams params() {
        return params;
    }

    @Override
    public JsonObject query() {
        JsonObject query = JsonObject.create().put("statement", statement);
        if (this.params != null) {
            this.params.injectParams(query);
        }
        return query;
    }
}
