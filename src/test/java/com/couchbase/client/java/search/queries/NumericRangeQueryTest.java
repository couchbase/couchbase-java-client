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

import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.search.SearchQuery;
import org.junit.Test;

public class NumericRangeQueryTest {

    @Test
    public void shouldFailIfNoBounds() {
        NumericRangeQuery fts = SearchQuery.numericRange();
        SearchQuery query = new SearchQuery("foo", fts);
        catchException(query).export();

        assertTrue(caughtException() instanceof NullPointerException);
        assertTrue(caughtException().getMessage().contains("min or max"));
    }

    @Test
    public void shouldAcceptMinOnly() {
        NumericRangeQuery fts = SearchQuery.numericRange().min(12.3);
        SearchQuery query = new SearchQuery("foo", fts);
        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("min", 12.3));
        assertEquals(expected, query.export());
    }

    @Test
    public void shouldAcceptMaxOnly() {
        NumericRangeQuery fts = SearchQuery.numericRange().max(12.3);
        SearchQuery query = new SearchQuery("foo", fts);
        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("max", 12.3));
        assertEquals(expected, query.export());
    }

    @Test
    public void shouldNotImplicitlySetDefaultsForInclusiveMinAndMax() {
        NumericRangeQuery fts = SearchQuery.numericRange()
            .boost(1.5)
            .field("field")
            .min(12.3)
            .max(4.5);
        SearchQuery query = new SearchQuery("foo", fts)
            .explain();

        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("min", 12.3)
                .put("max", 4.5)
                .put("boost", 1.5)
                .put("field", "field"))
            .put("explain", true);
        assertEquals(expected, query.export());
    }

    @Test
    public void shouldExportDateRangeQueryWithAllOptions() {
        NumericRangeQuery fts = SearchQuery.numericRange()
            .boost(1.5)
            .field("field")
            .min(12.3, false)
            .max(4.5, true);
        SearchQuery query = new SearchQuery("foo", fts)
            .explain();

        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("min", 12.3)
                .put("inclusive_min", false)
                .put("max", 4.5)
                .put("inclusive_max", true)
                .put("boost", 1.5)
                .put("field", "field"))
            .put("explain", true);
        assertEquals(expected, query.export());
    }

}