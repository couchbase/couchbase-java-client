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
public class NumericRangeQuery extends SearchQuery {
    public static final double BOOST = 1.0;
    private static final boolean INCLUSIVE_MIN = true;
    private static final boolean INCLUSIVE_MAX = false;

    private final double min;
    private final double max;
    private final boolean inclusiveMin;
    private final boolean inclusiveMax;
    private final String field;
    private final double boost;

    protected NumericRangeQuery(Builder builder) {
        super(builder);
        min = builder.min;
        max = builder.max;
        inclusiveMin = builder.inclusiveMin;
        inclusiveMax = builder.inclusiveMax;
        field = builder.field;
        boost = builder.boost;
    }

    public static Builder on(String index) {
        return new Builder(index);
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public boolean inclusiveMin() {
        return inclusiveMin;
    }

    public boolean inclusiveMax() {
        return inclusiveMax;
    }

    public String field() {
        return field;
    }

    public double boost() {
        return boost;
    }

    @Override
    public JsonObject queryJson() {
        return JsonObject.create()
                .put("min", min)
                .put("max", max)
                .put("inclusiveMin", inclusiveMin)
                .put("inclusiveMax", inclusiveMax)
                .put("field", field)
                .put("boost", boost);
    }

    public static class Builder extends SearchQuery.Builder {
        public double boost = BOOST;
        private double min;
        private double max;
        private boolean inclusiveMin = INCLUSIVE_MIN;
        private boolean inclusiveMax = INCLUSIVE_MAX;
        private String field;

        protected Builder(String index) {
            super(index);
        }

        public NumericRangeQuery build() {
            return new NumericRangeQuery(this);
        }

        public Builder boost(double boost) {
            this.boost = boost;
            return this;
        }

        public Builder min(double min) {
            this.min = min;
            return this;
        }

        public Builder max(double max) {
            this.max = max;
            return this;
        }

        public Builder inclusiveMin(boolean inclusiveMin) {
            this.inclusiveMin = inclusiveMin;
            return this;
        }

        public Builder inclusiveMax(boolean inclusiveMax) {
            this.inclusiveMax = inclusiveMax;
            return this;
        }

        public Builder field(String field) {
            this.field = field;
            return this;
        }
    }
}