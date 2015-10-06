/**
 * Copyright (C) 2015 Couchbase, Inc.
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
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