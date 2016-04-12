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
 * {@link DisjunctionQuery} creates a new compound Query.
 * Result documents satisfy <b>at least one</b> Query.
 *
 * @author Sergey Avseyev
 */
@InterfaceAudience.Public
@InterfaceStability.Experimental
public class DisjunctionQuery extends SearchQuery {
    private final SearchQuery[] disjuncts;

    protected DisjunctionQuery(Builder builder) {
        super(builder);
        disjuncts = builder.disjuncts;
    }

    public static Builder on(String index) {
        return new Builder(index);
    }

    public SearchQuery[] disjuncts() {
        return disjuncts;
    }

    @Override
    public JsonObject queryJson() {
        JsonArray disjunctsJson = JsonArray.create();
        for (SearchQuery disjunct : disjuncts) {
            disjunctsJson.add(disjunct.queryJson());
        }
        return JsonObject.create()
                .put("disjuncts", disjunctsJson);
    }

    public static class Builder extends SearchQuery.Builder {
        private SearchQuery[] disjuncts;

        protected Builder(String index) {
            super(index);
        }

        public DisjunctionQuery build() {
            return new DisjunctionQuery(this);
        }

        public Builder disjuncts(SearchQuery ...disjuncts) {
            this.disjuncts = disjuncts;
            return this;
        }
    }
}