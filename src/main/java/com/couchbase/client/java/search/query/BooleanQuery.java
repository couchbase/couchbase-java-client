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
import com.couchbase.client.java.document.json.JsonObject;

/**
 * {@link BooleanQuery} creates a compound Query composed of several
 * other Query objects. Result documents must satisfy <b>ALL</b> of
 * the <b>must</b> Queries. Result documents must satisfy <b>NONE</b>
 * of the <b>must not</b> Queries. If there are any <b>should</b>
 * queries, result documents must satisfy at least <b>one of them</b>.
 *
 * @author Sergey Avseyev
 */
@InterfaceAudience.Public
@InterfaceStability.Experimental
public class BooleanQuery extends SearchQuery {
    private final ConjunctionQuery must;
    private final ConjunctionQuery mustNot;
    private final ConjunctionQuery should;

    protected BooleanQuery(Builder builder) {
        super(builder);
        must = builder.must;
        mustNot = builder.mustNot;
        should = builder.should;
    }

    public static Builder on(String index) {
        return new Builder(index);
    }

    public ConjunctionQuery must() {
        return must;
    }

    public ConjunctionQuery mustNot() {
        return mustNot;
    }

    public ConjunctionQuery should() {
        return should;
    }

    @Override
    public JsonObject queryJson() {
        JsonObject json = JsonObject.create();

        if (must != null) {
            json.put("must", must.queryJson());
        }

        if (mustNot != null) {
            json.put("mustNot", mustNot.queryJson());
        }

        if (should != null) {
            json.put("should", should.queryJson());
        }
        return json;
    }

    public static class Builder extends SearchQuery.Builder {
        private ConjunctionQuery must;
        private ConjunctionQuery mustNot;
        private ConjunctionQuery should;

        protected Builder(String index) {
            super(index);
        }

        public BooleanQuery build() {
            return new BooleanQuery(this);
        }

        public Builder must(ConjunctionQuery must) {
            this.must = must;
            return this;
        }

        public Builder must(SearchQuery ...must) {
            this.must = ConjunctionQuery.on(index).conjuncts(must).build();
            return this;
        }

        public Builder mustNot(ConjunctionQuery mustNot) {
            this.mustNot = mustNot;
            return this;
        }

        public Builder mustNot(SearchQuery ...mustNot) {
            this.mustNot = ConjunctionQuery.on(index).conjuncts(mustNot).build();
            return this;
        }

        public Builder should(ConjunctionQuery should) {
            this.should = should;
            return this;
        }

        public Builder should(SearchQuery ...should) {
            this.should = ConjunctionQuery.on(index).conjuncts(should).build();
            return this;
        }
    }
}