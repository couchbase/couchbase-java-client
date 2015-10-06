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