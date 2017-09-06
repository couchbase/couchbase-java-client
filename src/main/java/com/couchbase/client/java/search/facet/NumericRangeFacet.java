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
package com.couchbase.client.java.search.facet;

import java.util.HashMap;
import java.util.Map;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;

/**
 * A facet that categorizes hits into numerical ranges (or buckets) provided by the user.
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Public
public class NumericRangeFacet extends SearchFacet {

    private static class NumericRange {
        public final Double min;
        public final Double max;

        public NumericRange(Double min, Double max) {
            this.min = min;
            this.max = max;
        }
    }

    private final Map<String, NumericRange> numericRanges;

    protected NumericRangeFacet(String field, int limit) {
        super(field, limit);
        this.numericRanges = new HashMap<String, NumericRange>();
    }

    protected void checkRange(String name, Double min, Double max) {
        if (name == null) {
            throw new NullPointerException("Cannot create numeric range without a name");
        }
        if (min == null && max == null) {
            throw new NullPointerException("Cannot create numeric range without min nor max");
        }
    }

    public NumericRangeFacet addRange(String name, Double min, Double max) {
        checkRange(name, min, max);
        this.numericRanges.put(name, new NumericRange(min, max));

        return this;
    }

    @Override
    public void injectParams(JsonObject queryJson) {
        super.injectParams(queryJson);

        JsonArray numericRange = JsonArray.empty();
        for (Map.Entry<String, NumericRange> nr : numericRanges.entrySet()) {
            JsonObject nrJson = JsonObject.create();
            nrJson.put("name", nr.getKey());

            if (nr.getValue().min != null) {
                nrJson.put("min", nr.getValue().min);
            }
            if (nr.getValue().max != null) {
                nrJson.put("max", nr.getValue().max);
            }

            numericRange.add(nrJson);
        }
        queryJson.put("numeric_ranges", numericRange);
    }
}
