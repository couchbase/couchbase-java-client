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

import static com.googlecode.catchexception.CatchException.verifyException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.search.SearchQuery;
import org.junit.Test;

public class PhraseQueryTest {

    @Test
    public void shouldExportPhraseQuery() {
        PhraseQuery fts = SearchQuery.phrase("salty", "beers");
        SearchQuery query = new SearchQuery("foo", fts);
        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create().put("terms", JsonArray.from("salty", "beers")));
        assertEquals(expected, query.export());
    }

    @Test(expected = NullPointerException.class)
    public void shouldNullPointerOnNullTermsDuringConstruction() {
        SearchQuery.phrase(null);
    }

    @Test
    public void shouldIllegalArgumentOnEmptyTermsDuringExport() {
        PhraseQuery fts = SearchQuery.phrase();
        SearchQuery query = new SearchQuery("foo", fts);
        assertNotNull(query);
        verifyException(query, IllegalArgumentException.class).export();
    }

    @Test
    public void shouldExportPhraseQueryWithAllOptions() {
        PhraseQuery fts = SearchQuery.phrase("salty", "beers")
            .boost(1.5)
            .field("field");
        SearchQuery query = new SearchQuery("foo", fts)
            .limit(10);

        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("terms", JsonArray.from("salty", "beers"))
                .put("boost", 1.5)
                .put("field", "field"))
            .put("size", 10);
        assertEquals(expected, query.export());
    }

}