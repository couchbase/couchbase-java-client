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