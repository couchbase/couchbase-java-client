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

package com.couchbase.client.java.search.query;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;

/**
 * {@link ConjunctionQuery} creates a new compound Query. Result documents
 * <b>must</b> satisfy <b>all</b> of the queries.
 *
 * @author Sergey Avseyev
 */
@InterfaceAudience.Public
@InterfaceStability.Experimental
public class ConjunctionQuery extends SearchQuery {
    private final SearchQuery[] conjuncts;

    protected ConjunctionQuery(Builder builder) {
        super(builder);
        conjuncts = builder.conjuncts;
    }

    public static Builder on(String index) {
        return new Builder(index);
    }

    public SearchQuery[] conjuncts() {
        return conjuncts;
    }

    @Override
    public JsonObject queryJson() {
        JsonArray conjunctsJson = JsonArray.create();
        for (SearchQuery conjunct : conjuncts) {
            conjunctsJson.add(conjunct.queryJson());
        }
        return JsonObject.create()
                .put("conjuncts", conjunctsJson);
    }

    public static class Builder extends SearchQuery.Builder {
        private SearchQuery[] conjuncts;

        protected Builder(String index) {
            super(index);
        }

        public ConjunctionQuery build() {
            return new ConjunctionQuery(this);
        }

        public Builder conjuncts(SearchQuery... conjuncts) {
            this.conjuncts = conjuncts;
            return this;
        }
    }
}