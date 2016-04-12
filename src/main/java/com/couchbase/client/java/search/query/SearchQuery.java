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
