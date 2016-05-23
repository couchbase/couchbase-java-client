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
import com.couchbase.client.java.search.SearchParams;
import com.couchbase.client.java.search.SearchQuery;
import org.junit.Test;

public class BooleanFieldQueryTest {

    @Test
    public void shouldExportBooleanFieldQuery() throws Exception {
        BooleanFieldQuery query = SearchQuery.booleanField(true);

        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("bool", true));
        assertEquals(expected, query.export());
    }

    @Test
    public void shouldExportBooleanFieldQueryWithAllOptions() {
        SearchParams params = SearchParams.build().limit(10);
        BooleanFieldQuery query = SearchQuery.booleanField(true)
            .boost(1.5)
            .field("field");

        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("bool", true)
                .put("boost", 1.5)
                .put("field", "field"))
            .put("size", 10);
        assertEquals(expected, query.export(params));
    }
}