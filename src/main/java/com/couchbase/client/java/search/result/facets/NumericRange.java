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
package com.couchbase.client.java.search.result.facets;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;

/**
 * A range (or bucket) for a {@link NumericRangeFacetResult}. Counts the number of matches
 * that fall into the named range (which can overlap with other user-defined ranges in the facet).
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class NumericRange {

    private final String name;
    private final Double min;
    private final Double max;
    private final long count;

    public NumericRange(String name, Double min, Double max, long count) {
        this.name = name;
        this.min = min;
        this.max = max;
        this.count = count;
    }

    public String name() {
        return name;
    }

    public Double min() {
        return min;
    }

    public Double max() {
        return max;
    }

    public long count() {
        return count;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("name='").append(name).append('\'');
        if (min != null) {
            sb.append(", min=").append(min);
        }
        if (max != null) {
            sb.append(", max=").append(max);
        }
        sb.append(", count=").append(count);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NumericRange that = (NumericRange) o;

        if (count != that.count) {
            return false;
        }
        if (!name.equals(that.name)) {
            return false;
        }
        if (min != null ? !min.equals(that.min) : that.min != null) {
            return false;
        }
        return max != null ? max.equals(that.max) : that.max == null;

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (min != null ? min.hashCode() : 0);
        result = 31 * result + (max != null ? max.hashCode() : 0);
        result = 31 * result + (int) (count ^ (count >>> 32));
        return result;
    }
}
