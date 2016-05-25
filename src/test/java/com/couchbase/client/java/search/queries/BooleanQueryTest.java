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

import static org.junit.Assert.assertEquals;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.search.SearchQuery;
import org.junit.Test;

public class BooleanQueryTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailExportWhenNoChild() {
        new SearchQuery("foo", SearchQuery.booleans()).export();
    }

    @Test
    public void shouldExportBooleanQueryWithInnerBoosts() {
        PrefixQuery inner = SearchQuery.prefix("someterm").boost(2);
        BooleanQuery fts = SearchQuery.booleans().must(inner).mustNot(inner).should(inner);
        SearchQuery query = new SearchQuery("foo", fts);

        JsonObject expectedInner = JsonObject.create().put("prefix", "someterm").put("boost", 2.0);
        JsonObject expectedMustNot = JsonObject.create().put("disjuncts", JsonArray.from(expectedInner));
        JsonObject expectedShould = JsonObject.create().put("disjuncts", JsonArray.from(expectedInner));
        JsonObject expectedMust = JsonObject.create().put("conjuncts", JsonArray.from(expectedInner));

        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("must", expectedMust)
                .put("must_not", expectedMustNot)
                .put("should", expectedShould));
        assertEquals(expected, query.export());
    }

    @Test
    public void shouldCumulateCallsToMust() {
        PrefixQuery inner1 = SearchQuery.prefix("someterm").boost(2);
        PrefixQuery inner2 = SearchQuery.prefix("otherterm");
        BooleanQuery fts = SearchQuery.booleans().must(inner1).must(inner2);
        SearchQuery query = new SearchQuery("foo", fts);

        JsonObject expectedInner1 = JsonObject.create().put("prefix", "someterm").put("boost", 2.0);
        JsonObject expectedInner2 = JsonObject.create().put("prefix", "otherterm");
        JsonObject expectedMust = JsonObject.create().put("conjuncts", JsonArray.from(expectedInner1, expectedInner2));

        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("must", expectedMust));
        assertEquals(expected, query.export());
    }

    @Test
    public void shouldCumulateCallsToMustNot() {
        PrefixQuery inner1 = SearchQuery.prefix("someterm").boost(2);
        PrefixQuery inner2 = SearchQuery.prefix("otherterm");
        BooleanQuery fts = SearchQuery.booleans().mustNot(inner1).mustNot(inner2);
        SearchQuery query = new SearchQuery("foo", fts);

        JsonObject expectedInner1 = JsonObject.create().put("prefix", "someterm").put("boost", 2.0);
        JsonObject expectedInner2 = JsonObject.create().put("prefix", "otherterm");
        JsonObject expectedMustNot = JsonObject.create().put("disjuncts", JsonArray.from(expectedInner1, expectedInner2));

        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("must_not", expectedMustNot));
        assertEquals(expected, query.export());
    }

    @Test
    public void shouldCumulateCallsToShould() {
        PrefixQuery inner1 = SearchQuery.prefix("someterm").boost(2);
        PrefixQuery inner2 = SearchQuery.prefix("otherterm");
        BooleanQuery fts = SearchQuery.booleans().should(inner1).should(inner2);
        SearchQuery query = new SearchQuery("foo", fts);

        JsonObject expectedInner1 = JsonObject.create().put("prefix", "someterm").put("boost", 2.0);
        JsonObject expectedInner2 = JsonObject.create().put("prefix", "otherterm");
        JsonObject expectedMust = JsonObject.create().put("disjuncts", JsonArray.from(expectedInner1, expectedInner2));

        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("should", expectedMust));
        assertEquals(expected, query.export());
    }

    @Test
    public void shouldUseMinForShould() {
        PrefixQuery inner = SearchQuery.prefix("someterm").boost(2);
        BooleanQuery fts = SearchQuery.booleans().must(inner).mustNot(inner).should(inner, inner).shouldMin(2);
        SearchQuery query = new SearchQuery("foo", fts);

        JsonObject expectedInner = JsonObject.create().put("prefix", "someterm").put("boost", 2.0);
        //default min from a disjunction query is omitted
        JsonObject expectedMustNot = JsonObject.create().put("disjuncts", JsonArray.from(expectedInner));

        JsonObject expectedShould = JsonObject.create()
                .put("disjuncts", JsonArray.from(expectedInner, expectedInner))
                //minShould sets the "should" section's min
                .put("min", 2);

        JsonObject expectedMust = JsonObject.create().put("conjuncts", JsonArray.from(expectedInner));

        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("must", expectedMust)
                .put("must_not", expectedMustNot)
                .put("should", expectedShould));
        assertEquals(expected, query.export());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailOnShouldSectionTooSmall() {
        PrefixQuery inner = SearchQuery.prefix("someterm").boost(2);
        BooleanQuery fts = SearchQuery.booleans().should(inner).shouldMin(2);
        SearchQuery query = new SearchQuery("foo", fts);

        query.export();
    }

    @Test
    public void shouldExportBooleanQueryWithAllOptions() {
        PrefixQuery innerA = SearchQuery.prefix("someterm").boost(2.0);
        PrefixQuery innerB = SearchQuery.prefix("termB");
        PrefixQuery innerC = SearchQuery.prefix("termC");

        BooleanQuery fts = SearchQuery.booleans()
            .boost(1.5)
            .must(innerA)
            .mustNot(innerB)
            .should(innerA, innerB, innerC)
            .shouldMin(3);
        SearchQuery query = new SearchQuery("foo", fts)
            .explain();

        JsonObject expectedInnerA = JsonObject.create().put("prefix", "someterm").put("boost", 2d);
        JsonObject expectedInnerB = JsonObject.create().put("prefix", "termB");
        JsonObject expectedInnerC = JsonObject.create().put("prefix", "termC");

        JsonObject expectedMust = JsonObject.create().put("conjuncts", JsonArray.from(expectedInnerA));
        JsonObject expectedMustNot = JsonObject.create()
                .put("disjuncts", JsonArray.from(expectedInnerB));
        JsonObject expectedShould = JsonObject.create()
                .put("disjuncts", JsonArray.from(expectedInnerA, expectedInnerB, expectedInnerC))
                .put("min", 3);

        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("boost", 1.5)
                .put("must", expectedMust)
                .put("must_not", expectedMustNot)
                .put("should", expectedShould))
            .put("explain", true);
        assertEquals(expected, query.export());
    }

}