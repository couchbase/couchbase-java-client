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

public class ConjunctionQueryTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailExportWhenNoChild() {
        new SearchQuery("foo", SearchQuery.disjuncts()).export();
    }

    @Test
    public void shouldExportConjunctionQueryWithInnerBoost() {
        PrefixQuery inner = SearchQuery.prefix("someterm").boost(2);
        ConjunctionQuery fts = SearchQuery.conjuncts(inner);
        SearchQuery query = new SearchQuery("foo", fts);

        JsonObject expectedInner = JsonObject.create().put("prefix", "someterm").put("boost", 2.0);
        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("conjuncts", JsonArray.from(expectedInner)));
        assertEquals(expected, query.export());
    }

    @Test
    public void shouldExportConjunctionQueryWithDefaults() {
        PrefixQuery inner = SearchQuery.prefix("someterm");
        ConjunctionQuery fts = SearchQuery.conjuncts(inner);
        SearchQuery query = new SearchQuery("foo", fts);

        JsonObject expectedInner = JsonObject.create().put("prefix", "someterm");
        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("conjuncts", JsonArray.from(expectedInner)));
        assertEquals(expected, query.export());
    }

    @Test
    public void shouldAddChildQueriesToExistingQueries() {
        PrefixQuery innerA = SearchQuery.prefix("someterm").boost(2.0);
        PrefixQuery innerB = SearchQuery.prefix("termB");
        PrefixQuery innerC = SearchQuery.prefix("termC");

        ConjunctionQuery fts = SearchQuery.conjuncts(innerA, innerB)
                .and(innerC);
        SearchQuery query = new SearchQuery("foo", fts);

        JsonObject expectedInnerA = JsonObject.create().put("prefix", "someterm").put("boost", 2d);
        JsonObject expectedInnerB = JsonObject.create().put("prefix", "termB");
        JsonObject expectedInnerC = JsonObject.create().put("prefix", "termC");
        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                    .put("conjuncts", JsonArray.from(expectedInnerA, expectedInnerB, expectedInnerC)));
        assertEquals(expected, query.export());
    }

    @Test
    public void shouldExportDisjunctionQueryWithAllOptions() {
        PrefixQuery innerA = SearchQuery.prefix("someterm").boost(2.0);
        PrefixQuery innerB = SearchQuery.prefix("termB");
        PrefixQuery innerC = SearchQuery.prefix("termC");

        ConjunctionQuery fts = SearchQuery.conjuncts()
            .boost(1.5)
            .and(innerA, innerB, innerC);
        SearchQuery query = new SearchQuery("foo", fts)
            .explain();

        JsonObject expectedInnerA = JsonObject.create().put("prefix", "someterm").put("boost", 2d);
        JsonObject expectedInnerB = JsonObject.create().put("prefix", "termB");
        JsonObject expectedInnerC = JsonObject.create().put("prefix", "termC");
        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("boost", 1.5)
                .put("conjuncts", JsonArray.from(expectedInnerA, expectedInnerB, expectedInnerC)))
            .put("explain", true);
        assertEquals(expected, query.export());
    }

}