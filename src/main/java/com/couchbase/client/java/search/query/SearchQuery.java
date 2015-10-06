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
import com.couchbase.client.java.search.SearchControl;

/**
 * @author Sergey Avseyev
 */
@InterfaceAudience.Public
@InterfaceStability.Experimental
public abstract class SearchQuery {
    public static final int SIZE = 10;
    public static final int FROM = 0;
    public static final double BOOST = 1.0;
    private static final boolean EXPLAIN = false;
    private static final String HIGHLIGHT_STYLE = "html"; /* html, ansi */
    protected final double boost;

    private final int size;
    private final int from;
    private final String index;
    private final boolean explain;
    private final String highlightStyle;
    private final String[] highlightFields;
    private final String[] fields;
    private final SearchControl control;

    protected SearchQuery(Builder builder) {
        size = builder.size;
        from = builder.from;
        index = builder.index;
        explain = builder.explain;
        highlightStyle = builder.highlightStyle;
        highlightFields = builder.highlightFields;
        fields = builder.fields;
        control = builder.control;
        boost = builder.boost;
    }

    public int limit() {
        return size;
    }

    public int offset() {
        return from;
    }

    public String index() {
        return index;
    }

    public String[] fields() {
        return fields;
    }

    public SearchControl control() {
        return control;
    }

    public double boost() {
        return boost;
    }

    public abstract JsonObject queryJson();

    public JsonObject json() {
        JsonObject json = JsonObject.create();
        json.put("query", queryJson().put("boost", boost));
        JsonObject highlightJson = JsonObject.create();
        if (highlightStyle != null) {
            highlightJson.put("style", highlightStyle);
        }
        if (highlightFields != null) {
            highlightJson.put("fields", JsonArray.from(highlightFields));
        }
        if (highlightJson.size() > 0) {
            json.put("highlight", highlightJson);
        }
        if (fields != null) {
            json.put("fields", JsonArray.from(fields));
        }
        json.put("size", size);
        json.put("from", from);
        json.put("explain", explain);
        if (control != null) {
            json.put("ctl", control.json());
        }
        return json;
    }

    public static abstract class Builder {
        public boolean explain = EXPLAIN;
        public String highlightStyle = HIGHLIGHT_STYLE;
        public String[] highlightFields;
        public String[] fields;
        public SearchControl control = null;
        public double boost = BOOST;
        private int size = SIZE;
        private int from = FROM;
        protected String index;

        protected Builder(String index) {
            this.index = index;
        }

        public abstract SearchQuery build();

        public Builder limit(int limit) {
            this.size = limit;
            return this;
        }

        public Builder offset(int offset) {
            this.from = offset;
            return this;
        }

        public Builder explain(boolean explain) {
            this.explain = explain;
            return this;
        }

        public Builder highlightStyle(String highlightStyle) {
            this.highlightStyle = highlightStyle;
            return this;
        }

        public Builder highlightFields(String... highlightFields) {
            this.highlightFields = highlightFields;
            return this;
        }

        public Builder fields(String... fields) {
            this.fields = fields;
            return this;
        }

        public Builder control(SearchControl control) {
            this.control = control;
            return this;
        }

        public Builder boost(double boost) {
            this.boost = boost;
            return this;
        }
    }
}
