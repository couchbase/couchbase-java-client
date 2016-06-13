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

import java.util.List;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;

/**
 * Implementation of a {@link DateRangeFacetResult}.
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class DefaultDateRangeFacetResult extends  AbstractFacetResult implements DateRangeFacetResult {

    private final List<DateRange> dateRanges;

    public DefaultDateRangeFacetResult(String name, String field, long total, long missing, long other,
            List<DateRange> dateRanges) {
        super(name, field, total, missing, other);
        this.dateRanges = dateRanges;
    }

    @Override
    public List<DateRange> dateRanges() {
        return this.dateRanges;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DateRangeFacetResult{")
                .append("name='").append(name).append('\'')
                .append(", field='").append(field).append('\'')
                .append(", total=").append(total)
                .append(", missing=").append(missing)
                .append(", other=").append(other)
                .append(", ranges=").append(dateRanges)
                .append('}');
        return sb.toString();
    }
}
