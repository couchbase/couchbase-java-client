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