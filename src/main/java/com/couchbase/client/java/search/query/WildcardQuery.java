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
 * @author Sergey Avseyev
 */
@InterfaceAudience.Public
@InterfaceStability.Experimental
public class WildcardQuery extends SearchQuery {
    private final String wildcard;
    private final String field;

    protected WildcardQuery(Builder builder) {
        super(builder);
        wildcard = builder.wildcard;
        field = builder.field;
    }

    public static Builder on(String index) {
        return new Builder(index);
    }

    public String wildcard() {
        return wildcard;
    }
    public String field() {
        return field;
    }

    @Override
    public JsonObject queryJson() {
        return JsonObject.create()
                .put("wildcard", wildcard)
                .put("field", field);
    }

    public static class Builder extends SearchQuery.Builder {
        private String wildcard;
        private String field;

        protected Builder(String index) {
            super(index);
        }

        public WildcardQuery build() {
            return new WildcardQuery(this);
        }

        public Builder wildcard(String wildcard) {
            this.wildcard = wildcard;
            return this;
        }

        public Builder field(String field) {
            this.field = field;
            return this;
        }
    }
}