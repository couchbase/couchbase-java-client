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

import java.util.Date;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.search.util.SearchUtils;

/**
 * A range (or bucket) for a {@link DateRangeFacetResult}. Counts the number of matches
 * that fall into the named range (which can overlap with other user-defined ranges).
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class DateRange {

    private final String name;
    private final Date start;
    private final Date end;
    private final long count;

    public DateRange(String name, String start, String end, long count) {
        this.name = name;
        this.count = count;

        this.start = SearchUtils.fromFtsString(start);
        this.end = SearchUtils.fromFtsString(end);
    }

    public String name() {
        return name;
    }

    public Date start() {
        return start;
    }

    public Date end() {
        return end;
    }

    public long count() {
        return count;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("name='").append(name).append('\'');
        if (start != null) {
            sb.append(", start='").append(start).append('\'');
        }
        if (end != null) {
            sb.append(", end='").append(end).append('\'');
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

        DateRange dateRange = (DateRange) o;

        if (count != dateRange.count) {
            return false;
        }
        if (!name.equals(dateRange.name)) {
            return false;
        }
        if (start != null ? !start.equals(dateRange.start) : dateRange.start != null) {
            return false;
        }
        return end != null ? end.equals(dateRange.end) : dateRange.end == null;

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (start != null ? start.hashCode() : 0);
        result = 31 * result + (end != null ? end.hashCode() : 0);
        result = 31 * result + (int) (count ^ (count >>> 32));
        return result;
    }
}
