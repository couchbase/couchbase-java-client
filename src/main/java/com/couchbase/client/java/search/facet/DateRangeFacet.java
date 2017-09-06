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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.search.util.SearchUtils;

/**
 * A facet that categorizes hits inside date ranges (or buckets) provided by the user.
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Public
public class DateRangeFacet extends SearchFacet {

    private final Map<String, DateRange> dateRanges;

    DateRangeFacet(String field, int limit) {
        super(field, limit);
        this.dateRanges = new HashMap<String, DateRange>();
    }

    protected void checkRange(String name, String start, String end) {
        if (name == null) {
            throw new NullPointerException("Cannot create date range without a name");
        }
        if (start == null && end == null) {
            throw new NullPointerException("Cannot create date range without start nor end");
        }
    }

    public DateRangeFacet addRange(String rangeName, Date start, Date end) {
        return addRange(rangeName, SearchUtils.toFtsUtcString(start), SearchUtils.toFtsUtcString(end));
    }

    public DateRangeFacet addRange(String rangeName, String start, String end) {
        checkRange(rangeName, start, end);
        this.dateRanges.put(rangeName, new DateRange(start, end));
        return this;
    }

    @Override
    public void injectParams(JsonObject queryJson) {
        super.injectParams(queryJson);

        JsonArray dateRange = JsonArray.empty();
        for (Map.Entry<String, DateRange> dr : dateRanges.entrySet()) {
            JsonObject drJson = JsonObject.create();
            drJson.put("name", dr.getKey());

            if (dr.getValue().start != null) {
                drJson.put("start", dr.getValue().start);
            }
            if (dr.getValue().end != null) {
                drJson.put("end", dr.getValue().end);
            }

            dateRange.add(drJson);
        }
        queryJson.put("date_ranges", dateRange);
    }

    private static class DateRange {

        public final String start;
        public final String end;

        public DateRange(String start, String end) {
            this.start = start;
            this.end = end;
        }
    }
}
