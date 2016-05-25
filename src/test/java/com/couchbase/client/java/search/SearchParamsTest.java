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
package com.couchbase.client.java.search;

import static com.googlecode.catchexception.CatchException.verifyException;
import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.search.facet.SearchFacet;
import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * Tests the global parameter part of a {@link SearchQuery}.
 */
public class SearchParamsTest {

    @Test
    public void shouldBeEmptyByDefault() {
        SearchQuery p = new SearchQuery(null, null);
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expected = JsonObject.empty();
        assertEquals(expected, result);
    }

    @Test
    public void shouldInjectLimit() {
        SearchQuery p = new SearchQuery(null, null).limit(10);
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expected = JsonObject.create().put("size", 10);
        assertEquals(expected, result);
    }

    @Test
    public void shouldInjectSkip() {
        SearchQuery p = new SearchQuery(null, null)
            .skip(100);
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expected = JsonObject.create().put("from", 100);
        assertEquals(expected, result);
    }

    @Test
    public void shouldInjectLimitAndSkip() {
        SearchQuery p = new SearchQuery(null, null)
            .limit(500)
            .skip(100);
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expected = JsonObject.create().put("from", 100).put("size", 500);
        assertEquals(expected, result);
    }

    @Test
    public void shouldInjectExplain() {
        SearchQuery p = new SearchQuery(null, null)
            .explain();
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expected = JsonObject.create().put("explain", true);
        assertEquals(expected, result);
    }

    @Test
    public void shouldInjectHighlightStyle() {
        SearchQuery p = new SearchQuery(null, null)
            .highlight(HighlightStyle.HTML);
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expected = JsonObject.create()
            .put("highlight", JsonObject.create().put("style", "html"));
        assertEquals(expected, result);
    }

    @Test
    public void shouldInjectHighlightStyleWithFields() {
        SearchQuery p = new SearchQuery(null, null)
            .highlight(HighlightStyle.ANSI, "foo", "bar");
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expected = JsonObject.create()
            .put("highlight", JsonObject.create().put("style", "ansi").put("fields", JsonArray.from("foo", "bar")));
        assertEquals(expected, result);
    }

    @Test
    public void shouldClearHighlightStyle() {
        SearchQuery p = new SearchQuery(null, null)
            .highlight(HighlightStyle.ANSI, "foo", "bar")
            .clearHighlight();
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expected = JsonObject.empty();
        assertEquals(expected, result);
    }

    @Test
    public void shouldInjectFields() {
        SearchQuery p = new SearchQuery(null, null)
            .fields("foo", "bar", "baz");
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expected = JsonObject.create()
            .put("fields", JsonArray.from("foo", "bar", "baz"));
        assertEquals(expected, result);
    }

    @Test
    public void shouldReplaceFieldsWhenCalledTwice() {
        SearchQuery p = new SearchQuery(null, null)
            .fields("foo", "bar", "baz")
            .fields("bingo");
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expected = JsonObject.create()
            .put("fields", JsonArray.from("bingo"));
        assertEquals(expected, result);
    }

    @Test
    public void shouldClearFieldsWhenCalledEmpty() {
        SearchQuery p = new SearchQuery(null, null)
            .fields("foo", "bar", "baz")
            .fields();
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expected = JsonObject.empty();
        assertEquals(expected, result);
    }

    @Test
    public void shouldInjectFacets() {
        SearchQuery p = new SearchQuery(null, null)
            .addFacets(
                SearchFacet.term("term", "somefield", 10),
                SearchFacet.date("dr", "datefield", 1).addRange("name", "start", "end"),
                SearchFacet.numeric("nr", "numfield", 99).addRange("name2", 0.0, 99.99)
            );
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject term = JsonObject.create().put("size", 10).put("field", "somefield");
        JsonObject nr = JsonObject.create()
            .put("size", 99)
            .put("field", "numfield")
            .put("numeric_ranges", JsonArray.from(
                JsonObject.create().put("name", "name2").put("max", 99.99).put("min", 0.0))
            );
        JsonObject dr = JsonObject.create()
            .put("size", 1)
            .put("field", "datefield")
            .put("date_ranges", JsonArray.from(
                JsonObject.create().put("name", "name").put("start", "start").put("end", "end"))
            );
        JsonObject expected = JsonObject.create()
            .put("facets", JsonObject.create()
                .put("nr", nr)
                .put("dr", dr)
                .put("term", term)
            );
        assertEquals(expected, result);
    }

    @Test
    public void shouldAddFacetsToExistingFacets() {
        SearchQuery p = new SearchQuery(null, null)
                .addFacets(SearchFacet.term("A", "field1", 1))
                .addFacets(SearchFacet.term("B", "field2", 2));

        JsonObject result = JsonObject.create();
        p.injectParams(result);

        JsonObject expected = JsonObject.create()
            .put("facets", JsonObject.create()
                .put("A", JsonObject.create().put("field", "field1").put("size", 1))
                .put("B", JsonObject.create().put("field", "field2").put("size", 2))
            );
        assertEquals(expected, result);
    }

    @Test
    public void shouldReplaceExistingFacetWithSameName() {
        SearchQuery p = new SearchQuery(null, null)
                .addFacets(SearchFacet.term("A", "field1", 1))
                .addFacets(SearchFacet.term("A", "field2", 2));

        JsonObject result = JsonObject.create();
        p.injectParams(result);

        JsonObject expected = JsonObject.create()
            .put("facets", JsonObject.create()
                    .put("A", JsonObject.create()
                            .put("field", "field2").put("size", 2))
            );
        assertEquals(expected, result);
    }

    @Test
    public void shouldClearExistingFacets() {
        SearchQuery p = new SearchQuery(null, null)
                .addFacets(SearchFacet.term("A", "field1", 1))
                .clearFacets()
                .addFacets(SearchFacet.term("B", "field2", 2));

        JsonObject result = JsonObject.create();
        p.injectParams(result);

        JsonObject expected = JsonObject.create()
            .put("facets", JsonObject.create()
                .put("B", JsonObject.create().put("field", "field2").put("size", 2))
            );
        assertEquals(expected, result);
    }

    @Test
    public void shouldThrowOnDateRangeWithoutName() {
        verifyException(SearchFacet.date("facet", "field", 1).addRange("rangeName", "", ""), NullPointerException.class)
                .addRange(null, "a", "b"); //where the exception is expected
    }

    @Test
    public void shouldThrowOnDateRangeWithoutBoundaries() {
        verifyException(SearchFacet.date("facet", "field", 1).addRange("rangeName", "", ""), NullPointerException.class)
                .addRange("name", (String) null, null); //where the exception is expected

        verifyException(SearchFacet.date("facet", "field", 1).addRange("rangeName", "", ""), NullPointerException.class)
                .addRange("name", (Date) null, null); //where the exception is expected
    }

    @Test
    public void shouldThrowOnNumericRangeWithoutName() {
        verifyException(SearchFacet.numeric("facet", "field", 1).addRange("rangeName", 0d, 0d), NullPointerException.class)
                .addRange(null, 1.2, 3.4); //where the exception is expected
    }

    @Test
    public void shouldThrowOnNumericRangeWithoutBoundaries() {
        verifyException(SearchFacet.numeric("facet", "field", 1).addRange("rangeName", 0d, 0d), NullPointerException.class)
                .addRange("name", null, null); //where the exception is expected
    }

    @Test
    public void shouldAllowOneNullBoundOnDateRange() {
        SearchFacet.date("facet", "field", 1)
                .addRange("rangeName", "", "")
                .addRange("name", "a", null);
        SearchFacet.date("facet", "field", 1)
                .addRange("rangeName", "", "")
                .addRange("name", null, "b");
    }

    @Test
    public void shouldAllowOneNullBoundOnNumericRange() {
        SearchFacet.numeric("facet", "field", 1)
                .addRange("rangeName", 0d, 0d)
                .addRange("name", 1.2, null);
        SearchFacet.numeric("facet", "field", 1)
                .addRange("rangeName", 0d, 0d)
                .addRange("name", null, 3.4);
    }

    @Test
    public void shouldConvertDatesToUtcStringInDateFacet() {
        Calendar start = Calendar.getInstance(TimeZone.getTimeZone("GMT-8:00"));
        start.clear();
        start.set(2016, Calendar.FEBRUARY, 3, 8, 45, 1);
        Calendar end = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        end.clear();
        end.set(2016, Calendar.FEBRUARY, 3, 16, 46, 1);
        SearchQuery params = new SearchQuery(null, null).addFacets(
                SearchFacet.date("facet", "field", 1).addRange("date", start.getTime(), end.getTime()));
        JsonObject json = JsonObject.create();
        params.injectParams(json);

        String expectedStart = "2016-02-03T16:45:01Z";
        String expectedEnd = "2016-02-03T16:46:01Z";
        JsonObject expected = JsonObject.create().put("facets", JsonObject.create().put("facet", JsonObject.create()
            .put("size", 1)
            .put("field", "field")
            .put("date_ranges", JsonArray.from(
                    JsonObject.create()
                    .put("name", "date")
                    .put("start", expectedStart)
                    .put("end", expectedEnd)
            ))));

        Assertions.assertThat(json).isEqualTo(expected);
    }

    @Test
    public void shouldInjectServerSideTimeout() {
        SearchQuery p = new SearchQuery(null, null)
            .serverSideTimeout(3, TimeUnit.SECONDS);
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expected = JsonObject.create()
            .put("ctl", JsonObject.create().put("timeout", 3000L));
        assertEquals(expected, result);
    }

}