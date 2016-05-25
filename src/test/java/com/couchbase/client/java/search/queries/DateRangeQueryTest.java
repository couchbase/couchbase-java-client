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
package com.couchbase.client.java.search.queries;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.TimeZone;

import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.search.SearchQuery;
import org.junit.Test;

public class DateRangeQueryTest {

    @Test
    public void shouldFailIfNoBounds() {
        DateRangeQuery fts = SearchQuery.dateRange();
        SearchQuery query = new SearchQuery("foo", fts);
        catchException(query).export();

        assertTrue(caughtException() instanceof NullPointerException);
        assertTrue(caughtException().getMessage().contains("start or end"));
    }

    @Test
    public void shouldAcceptEndOnly() {
        DateRangeQuery fts = SearchQuery.dateRange().end("theEnd");
        SearchQuery query = new SearchQuery("foo", fts);
        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("end", "theEnd"));
        assertEquals(expected, query.export());
    }

    @Test
    public void shouldAcceptStartOnly() {
        DateRangeQuery fts = SearchQuery.dateRange().start("theStart");
        SearchQuery query = new SearchQuery("foo", fts);
        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("start", "theStart"));
        assertEquals(expected, query.export());
    }

    @Test
    public void shouldNotImplicitlySetDefaultsForInclusiveStartAndEnd() {
        DateRangeQuery fts = SearchQuery.dateRange()
            .boost(1.5)
            .field("field")
            .start("a")
            .end("b")
            .dateTimeParser("parser");
        SearchQuery query = new SearchQuery("foo", fts)
            .explain();

        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("start", "a")
                .put("end", "b")
                .put("datetime_parser", "parser")
                .put("boost", 1.5)
                .put("field", "field"))
            .put("explain", true);
        assertEquals(expected, query.export());
    }

    @Test
    public void shouldIgnoreInclusiveStartWithNullStart() {
        DateRangeQuery fts = SearchQuery.dateRange()
            .start((String) null, true)
            .end("b");
        SearchQuery query = new SearchQuery("foo", fts);

        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("end", "b"));
        assertEquals(expected, query.export());
    }

    @Test
    public void shouldIgnoreInclusiveEndWithNullEnd() {
        DateRangeQuery fts = SearchQuery.dateRange()
            .start("a")
            .end((String) null, true);
        SearchQuery query = new SearchQuery("foo", fts);

        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("start", "a"));
        assertEquals(expected, query.export());
    }

    @Test
    public void shouldConvertDateToUtcString() {
        Calendar start = Calendar.getInstance(TimeZone.getTimeZone("GMT-8:00"));
        start.clear();
        start.set(2016, Calendar.FEBRUARY, 3, 8, 45, 1);
        Calendar end = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        end.clear();
        end.set(2016, Calendar.FEBRUARY, 3, 16, 46, 1);

        String expectedStart = "2016-02-03T16:45:01Z";
        String expectedEnd = "2016-02-03T16:46:01Z";

        DateRangeQuery fts = SearchQuery.dateRange()
            .start(start.getTime(), false)
            .end(end.getTime());
        SearchQuery query = new SearchQuery("foo", fts);

        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("start", expectedStart)
                .put("inclusive_start", false)
                .put("end", expectedEnd));
        assertEquals(expected, query.export());
    }

    @Test
    public void shouldExportDateRangeQueryWithAllOptions() {
        DateRangeQuery fts = SearchQuery.dateRange()
            .boost(1.5)
            .field("field")
            .start("a", false)
            .end("b", true)
            .dateTimeParser("parser");
        SearchQuery query = new SearchQuery("foo", fts)
            .explain();

        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("start", "a")
                .put("inclusive_start", false)
                .put("end", "b")
                .put("inclusive_end", true)
                .put("datetime_parser", "parser")
                .put("boost", 1.5)
                .put("field", "field"))
            .put("explain", true);
        assertEquals(expected, query.export());
    }

}