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

import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.search.SearchQuery;
import org.junit.Test;

public class QueryStringQueryTest {

    @Test
    public void shouldBuildStringQueryWithoutParams() {
        QueryStringQuery fts = SearchQuery.queryString("description:water and some other stuff");
        SearchQuery query = new SearchQuery("foo", fts);
        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create().put("query", "description:water and some other stuff"));
        assertEquals(expected, query.export());
    }

    @Test
    public void shouldBuildStringQueryWithParamsAndBoost() {
        QueryStringQuery fts = SearchQuery.queryString("q*ry").boost(2.0);
        SearchQuery query = new SearchQuery("foo", fts)
            .explain()
            .limit(10);
        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("query", "q*ry")
                .put("boost", 2.0))
            .put("explain", true)
            .put("size", 10);
        assertEquals(expected, query.export());
    }

}