/*
 * Copyright (c) 2018 Couchbase, Inc.
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
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;

/**
 * This query type extends the simple one so that it properly serializes parameterized
 * queries, both named and/or positional ones.
 *
 * @since 2.6.2
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public class ParameterizedAnalyticsQuery extends SimpleAnalyticsQuery {

    private final JsonArray positional;
    private final JsonObject named;

    ParameterizedAnalyticsQuery(String statement, JsonArray positional, JsonObject named,
        AnalyticsParams params) {
        super(statement, params);
        this.named = named;
        this.positional = positional;
    }

    @Override
    public JsonObject query() {
        JsonObject query = super.query();

        if (named != null && !named.isEmpty()) {
            for (String key : named.getNames()) {
                Object value = named.get(key);
                if (!key.startsWith("$")) {
                    key = "$" + key;
                }
                query.put(key, value);
            }
        }
        if (positional != null && !positional.isEmpty()) {
            query.put("args", positional);
        }
        return query;
    }
}
