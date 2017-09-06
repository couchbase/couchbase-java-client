/*
 * Copyright (c) 2017 Couchbase, Inc.
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
package com.couchbase.client.java.search.queries;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonObject;

/**
 * A FTS query that matches documents on a range of values. At least one bound is required, and the
 * inclusiveness of each bound can be configured.
 *
 * @author Michael Nitschinger
 * @since 2.4.5
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Public
public class TermRangeQuery extends AbstractFtsQuery {

    private String min;
    private String max;
    private Boolean inclusiveMin = null;
    private Boolean inclusiveMax = null;
    private String field;

    public TermRangeQuery() {
        super();
    }

    /**
     * Sets the lower boundary of the range, inclusive or not depending on the second parameter.
     */
    public TermRangeQuery min(String min, boolean inclusive) {
        this.min = min;
        this.inclusiveMin = inclusive;
        return this;
    }

    /**
     * Sets the lower boundary of the range.
     * The lower boundary is considered inclusive by default on the server side.
     * @see #min(String, boolean)
     */
    public TermRangeQuery min(String min) {
        this.min = min;
        this.inclusiveMin = null;
        return this;
    }

    /**
     * Sets the upper boundary of the range, inclusive or not depending on the second parameter.
     */
    public TermRangeQuery max(String max, boolean inclusive) {
        this.max = max;
        this.inclusiveMax = inclusive;
        return this;
    }

    /**
     * Sets the upper boundary of the range.
     * The upper boundary is considered exclusive by default on the server side.
     * @see #max(String, boolean)
     */
    public TermRangeQuery max(String max) {
        this.max = max;
        this.inclusiveMax = null;
        return this;
    }

    public TermRangeQuery field(String field) {
        this.field = field;
        return this;
    }

    @Override
    public TermRangeQuery boost(double boost) {
        super.boost(boost);
        return this;
    }

    @Override
    protected void injectParams(JsonObject input) {
        if (min == null && max == null) {
            throw new NullPointerException("TermRangeQuery needs at least one of min or max");
        }
        if (min != null) {
            input.put("min", min);
            if (inclusiveMin != null) {
                input.put("inclusive_min", inclusiveMin);
            }
        }
        if (max != null) {
            input.put("max", max);
            if (inclusiveMax != null) {
                input.put("inclusive_max", inclusiveMax);
            }
        }
        if (field != null) {
            input.put("field", field);
        }
    }
}
