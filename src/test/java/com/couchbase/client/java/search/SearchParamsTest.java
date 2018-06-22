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

import static com.couchbase.client.java.search.sort.SearchSort.sortField;
import static com.couchbase.client.java.search.sort.SearchSort.sortGeoDistance;
import static com.couchbase.client.java.search.sort.SearchSort.sortId;
import static com.couchbase.client.java.search.sort.SearchSort.sortScore;
import static com.googlecode.catchexception.CatchException.verifyException;
import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.search.facet.SearchFacet;
import com.couchbase.client.java.search.sort.FieldMissing;
import com.couchbase.client.java.search.sort.FieldMode;
import com.couchbase.client.java.search.sort.FieldType;
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
    public void shouldHaveHighlightButNoStyleWhenServerDefault() {
        SearchQuery p = new SearchQuery(null, null)
            .highlight(HighlightStyle.SERVER_DEFAULT);
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expected = JsonObject.create()
            .put("highlight", JsonObject.empty());
        assertEquals(expected, result);
    }

    @Test
    public void testHighlightWithoutParamHasServerDefaultStyle() {
        SearchQuery a = new SearchQuery(null, null)
            .highlight(HighlightStyle.SERVER_DEFAULT);
        SearchQuery b = new SearchQuery(null, null).highlight();

        JsonObject resultA = JsonObject.create();
        a.injectParams(resultA);
        JsonObject resultB = JsonObject.create();
        b.injectParams(resultB);

        assertEquals(resultA, resultB);
    }

    @Test
    public void testHighlightWithFieldsOnlyHasServerDefaultStyle() {
        SearchQuery a = new SearchQuery(null, null)
            .highlight(HighlightStyle.SERVER_DEFAULT, "foo", "bar");
        SearchQuery b = new SearchQuery(null, null).highlight("foo", "bar");

        JsonObject resultA = JsonObject.create();
        a.injectParams(resultA);
        JsonObject resultB = JsonObject.create();
        b.injectParams(resultB);
        JsonObject expected = JsonObject.create()
            .put("highlight", JsonObject.create().put("fields", JsonArray.from("foo", "bar")));

        assertEquals(expected, resultA);
        assertEquals(resultA, resultB);
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
            .addFacet("term", SearchFacet.term("somefield", 10))
            .addFacet("dr", SearchFacet.date("datefield", 1).addRange("name", "start", "end"))
            .addFacet("nr", SearchFacet.numeric("numfield", 99).addRange("name2", 0.0, 99.99));
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
                .addFacet("A", SearchFacet.term("field1", 1))
                .addFacet("B", SearchFacet.term("field2", 2));

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
                .addFacet("A", SearchFacet.term("field1", 1))
                .addFacet("A", SearchFacet.term("field2", 2));

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
                .addFacet("A", SearchFacet.term("field1", 1))
                .clearFacets()
                .addFacet("B", SearchFacet.term("field2", 2));

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
        verifyException(SearchFacet.date("field", 1).addRange("rangeName", "", ""), NullPointerException.class)
                .addRange(null, "a", "b"); //where the exception is expected
    }

    @Test
    public void shouldThrowOnDateRangeWithoutBoundaries() {
        verifyException(SearchFacet.date("field", 1).addRange("rangeName", "", ""), NullPointerException.class)
                .addRange("name", (String) null, null); //where the exception is expected

        verifyException(SearchFacet.date("field", 1).addRange("rangeName", "", ""), NullPointerException.class)
                .addRange("name", (Date) null, null); //where the exception is expected
    }

    @Test
    public void shouldThrowOnNumericRangeWithoutName() {
        verifyException(SearchFacet.numeric("field", 1).addRange("rangeName", 0d, 0d), NullPointerException.class)
                .addRange(null, 1.2, 3.4); //where the exception is expected
    }

    @Test
    public void shouldThrowOnNumericRangeWithoutBoundaries() {
        verifyException(SearchFacet.numeric("field", 1).addRange("rangeName", 0d, 0d), NullPointerException.class)
                .addRange("name", null, null); //where the exception is expected
    }

    @Test
    public void shouldAllowOneNullBoundOnDateRange() {
        SearchFacet.date("field", 1)
                .addRange("rangeName", "", "")
                .addRange("name", "a", null);
        SearchFacet.date("field", 1)
                .addRange("rangeName", "", "")
                .addRange("name", null, "b");
    }

    @Test
    public void shouldAllowOneNullBoundOnNumericRange() {
        SearchFacet.numeric("field", 1)
                .addRange("rangeName", 0d, 0d)
                .addRange("name", 1.2, null);
        SearchFacet.numeric("field", 1)
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
        SearchQuery params = new SearchQuery(null, null)
                .addFacet("facet", SearchFacet.date("field", 1).addRange("date", start.getTime(), end.getTime()));
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

    @Test
    public void shouldInjectEmptyConsistencyLevel() {
        SearchQuery p = new SearchQuery(null, null)
                .searchConsistency(SearchConsistency.NOT_BOUNDED);
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expected = JsonObject.create()
                .put("ctl",
                        JsonObject.create().put("consistency",
                                JsonObject.create()
                                .put("level", "")));
        assertEquals(expected, result);
    }

    @Test
    public void shouldInjectAtPlusConsistencyLevelWithVectors() {
        MutationToken token1 = new MutationToken(1, 1234, 1000, "bucket1");
        MutationToken token2 = new MutationToken(2, 1235, 2000, "bucket1");
        JsonDocument doc1 = JsonDocument.create("id", 0, JsonObject.empty(), 0, token1);
        JsonDocument doc2 = JsonDocument.create("id", 0, JsonObject.empty(), 0, token2);

        SearchQuery p = new SearchQuery("foo", null)
                .consistentWith(doc1, doc2);
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expectedVector = JsonObject.create().put("foo", JsonObject.create()
            .put("1/1234", 1000L).put("2/1235", 2000L));
        JsonObject expected = JsonObject.create()
                .put("ctl",
                        JsonObject.create().put("consistency",
                                JsonObject.create()
                                .put("level", "at_plus")
                                .put("vectors", expectedVector)));
        assertEquals(expected, result);
    }

    @Test
    public void shouldReplaceConsistencyWithNotBounded() {
        MutationToken token1 = new MutationToken(1, 1234, 1000, "bucket1");
        MutationToken token2 = new MutationToken(2, 1235, 2000, "bucket1");
        JsonDocument doc1 = JsonDocument.create("id", 0, JsonObject.empty(), 0, token1);
        JsonDocument doc2 = JsonDocument.create("id", 0, JsonObject.empty(), 0, token2);

        SearchQuery p = new SearchQuery(null, null)
                .consistentWith(doc1, doc2)
                .searchConsistency(SearchConsistency.NOT_BOUNDED);
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expected = JsonObject.create()
                .put("ctl",
                        JsonObject.create().put("consistency",
                                JsonObject.create()
                                .put("level", "")));
        assertEquals(expected, result);
    }

    @Test
    public void shouldReplaceConsistencyWithAtPlus() {
        MutationToken token1 = new MutationToken(1, 1234, 1000, "bucket1");
        MutationToken token2 = new MutationToken(2, 1235, 2000, "bucket1");
        JsonDocument doc1 = JsonDocument.create("id", 0, JsonObject.empty(), 0, token1);
        JsonDocument doc2 = JsonDocument.create("id", 0, JsonObject.empty(), 0, token2);

        SearchQuery p = new SearchQuery("foo", null)
                .searchConsistency(SearchConsistency.NOT_BOUNDED)
                .consistentWith(doc1, doc2);
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expectedVector = JsonObject.create().put("foo", JsonObject.create()
            .put("1/1234", 1000L).put("2/1235", 2000L));
        JsonObject expected = JsonObject.create()
                .put("ctl",
                        JsonObject.create().put("consistency",
                                JsonObject.create()
                                .put("level", "at_plus")
                                .put("vectors", expectedVector)));
        assertEquals(expected, result);
    }

    @Test
    public void shouldReplaceOnConsistentWithOnSameVbucketLargerSeqno() {
        MutationToken token1 = new MutationToken(1, 1234, 1000, "bucket1");
        MutationToken token2 = new MutationToken(1, 123, 1001, "bucket1");
        JsonDocument doc1 = JsonDocument.create("id", 0, JsonObject.empty(), 0, token1);
        JsonDocument doc2 = JsonDocument.create("id", 0, JsonObject.empty(), 0, token2);

        SearchQuery p = new SearchQuery("foo", null)
                .searchConsistency(SearchConsistency.NOT_BOUNDED)
                .consistentWith(doc1, doc2);
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expectedVector = JsonObject.create().put("foo", JsonObject.create().put("1/123", 1001L));
        JsonObject expected = JsonObject.create()
                .put("ctl",
                        JsonObject.create().put("consistency",
                                JsonObject.create()
                                .put("level", "at_plus")
                                .put("vectors", expectedVector)));
        assertEquals(expected, result);
    }

    @Test
    public void shouldNotReplaceOnConsistentWithOnSameVbucketLesserSeqno() {
        MutationToken token1 = new MutationToken(1, 1234, 1000, "bucket1");
        MutationToken token2 = new MutationToken(1, 1235, 8, "bucket1");
        JsonDocument doc1 = JsonDocument.create("id", 0, JsonObject.empty(), 0, token1);
        JsonDocument doc2 = JsonDocument.create("id", 0, JsonObject.empty(), 0, token2);

        SearchQuery p = new SearchQuery("foo", null)
                .searchConsistency(SearchConsistency.NOT_BOUNDED)
                .consistentWith(doc1, doc2);
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expectedVector = JsonObject.create().put("foo", JsonObject.create().put("1/1234", 1000L));
        JsonObject expected = JsonObject.create()
                .put("ctl",
                        JsonObject.create().put("consistency",
                                JsonObject.create()
                                .put("level", "at_plus")
                                .put("vectors", expectedVector)));
        assertEquals(expected, result);
    }

    @Test
    public void shouldInjectSort() {
        SearchQuery p = new SearchQuery(null, null)
            .sort("hello", "world", "-_score");
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expected = JsonObject.create()
            .put("sort", JsonArray.from("hello", "world", "-_score"));
        assertEquals(expected, result);
    }

    @Test
    public void shouldInjectAdvancedScoreSort() {
        SearchQuery p = new SearchQuery(null, null)
            .sort(sortScore(), sortScore().descending(true));
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expected = JsonObject.create()
            .put("sort", JsonArray.from(
                JsonObject.create().put("by", "score"),
                JsonObject.create().put("by", "score").put("desc", true)
            ));
        assertEquals(expected, result);
    }

    @Test
    public void shouldInjectAdvancedIdSort() {
        SearchQuery p = new SearchQuery(null, null)
            .sort(sortId(), sortId().descending(true));
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expected = JsonObject.create()
            .put("sort", JsonArray.from(
                JsonObject.create().put("by", "id"),
                JsonObject.create().put("by", "id").put("desc", true)
            ));
        assertEquals(expected, result);
    }

    @Test
    public void shouldInjectAdvancedFieldSort() {
        SearchQuery p = new SearchQuery(null, null)
            .sort(sortField("fieldname"), sortField("f")
                .missing(FieldMissing.FIRST).mode(FieldMode.DEFAULT).type(FieldType.AUTO)
                .descending(true));
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expected = JsonObject.create()
            .put("sort", JsonArray.from(
                JsonObject.create().put("by", "field").put("field", "fieldname"),
                JsonObject.create().put("by", "field").put("field", "f")
                    .put("desc", true).put("mode", "default").put("missing", "first")
                    .put("type", "auto")
            ));
        assertEquals(expected, result);
    }

    @Test
    public void shouldInjectAdvancedGeoSort() {
        SearchQuery p = new SearchQuery(null, null)
            .sort(sortGeoDistance(1.0, 2.0, "fname")
                .unit("km").descending(true));
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expected = JsonObject.create()
            .put("sort", JsonArray.from(
                JsonObject.create().put("by", "geo_distance").put("field", "fname")
                    .put("desc", true).put("unit", "km")
                    .put("location", JsonArray.from(1.0, 2.0))
            ));
        assertEquals(expected, result);
    }

    @Test
    public void shouldInjectMixedSort() {
        SearchQuery p = new SearchQuery(null, null)
            .sort("_score", sortId().descending(true), "bar");
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expected = JsonObject.create()
            .put("sort", JsonArray.from(
                "_score",
                JsonObject.create().put("by", "id").put("desc", true),
                "bar"
            ));
        assertEquals(expected, result);
    }

    @Test
    public void shouldInjectRawSort() {
        SearchQuery p = new SearchQuery(null, null)
            .sort(JsonObject.create().put("by", "new_stuff"));
        JsonObject result = JsonObject.empty();
        p.injectParams(result);

        JsonObject expected = JsonObject.create()
            .put("sort", JsonArray.from(JsonObject.create().put("by", "new_stuff")));
        assertEquals(expected, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailAdvancedSortOnUnsupportedType() {
        SearchQuery p = new SearchQuery(null, null)
            .sort(123);
        JsonObject result = JsonObject.empty();
        p.injectParams(result);
    }

}