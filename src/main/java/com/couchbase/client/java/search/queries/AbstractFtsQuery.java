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
package com.couchbase.client.java.search.queries;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.search.SearchParams;
import com.couchbase.client.java.search.SearchQuery;

/**
 * A base class for all FTS query classes. Exposes the common FTS query parameters.
 * In order to instantiate various flavors of queries, look at concrete classes or
 * static factory methods in {@link SearchQuery}.
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Experimental
@InterfaceAudience.Private
public abstract class AbstractFtsQuery {

    private Double boost;

    protected AbstractFtsQuery() { }

    public AbstractFtsQuery boost(double boost) {
        this.boost = boost;
        return this;
    }

    /**
     * Export the FTS query in its JSON format, not taking any global level {@link SearchParams parameters} into account.
     *
     * @return the FTS query JSON.
     */
    public JsonObject export() {
        return export(null);
    }

    /**
     * Export the FTS query in its JSON format, taking global level {@link SearchParams parameters} into account.
     *
     * @param searchParams the global level parameters for the search.
     * @return the FTS query JSON.
     */
    public JsonObject export(SearchParams searchParams) {
        JsonObject result = JsonObject.create();
        if (searchParams != null) {
            searchParams.injectParams(result);
        }
        JsonObject query = JsonObject.create();
        injectParamsAndBoost(query);
        return result.put("query", query);
    }

    /**
     * Injects the query's parameters (including the common boost and query-specific parameters)
     * into a prepared {@link JsonObject}.
     *
     * @param input the prepared JsonObject to receive the parameters.
     * @see #export(SearchParams) for a usage of this method.
     */
    public void injectParamsAndBoost(JsonObject input) {
        if (boost != null) {
            input.put("boost", boost);
        }
        injectParams(input);
    }

    /**
     * Override to inject query-specific parameters when doing the {@link #export(SearchParams)}.
     *
     * @param input the prepared {@link JsonObject} that will represent the query.
     */
    protected abstract void injectParams(JsonObject input);

    /**
     * @return the String representation of the FTS query, which is its JSON representation without global parameters.
     */
    @Override
    public String toString() {
        return export(null).toString();
    }

}
