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

public class TermQueryTest {

    @Test
    public void shouldExportTermQuery() {
        TermQuery query = SearchQuery.term("salty");
        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create().put("term", "salty"));
        assertEquals(expected, query.export());
    }

    @Test
    public void shouldOmitPrefixLengthWhenNoFuzziness() {
        SearchParams params = SearchParams.build().limit(10);
        TermQuery query = SearchQuery.term("salty")
            .boost(1.5)
            .field("field")
            .prefixLength(12);

        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("term", "salty")
                .put("boost", 1.5)
                .put("field", "field")) //no prefix_length
            .put("size", 10);
        assertEquals(expected, query.export(params));
    }

    @Test
    public void shouldOmitPrefixLengthUnderOne() {
        SearchParams params = SearchParams.build().limit(10);
        TermQuery query = SearchQuery.term("salty")
            .boost(1.5)
            .field("field")
            .fuzziness(23)
            .prefixLength(-1);

        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("term", "salty")
                .put("boost", 1.5)
                .put("field", "field")
                .put("fuzziness", 23))
            .put("size", 10);
        assertEquals(expected, query.export(params));
    }

    @Test
    public void shouldExportTermQueryWithAllOptions() {
        SearchParams params = SearchParams.build().limit(10);
        TermQuery query = SearchQuery.term("salty")
            .boost(1.5)
            .field("field")
            .fuzziness(23)
            .prefixLength(12);

        JsonObject expected = JsonObject.create()
            .put("query", JsonObject.create()
                .put("term", "salty")
                .put("boost", 1.5)
                .put("field", "field")
                .put("fuzziness", 23)
                .put("prefix_length", 12))
            .put("size", 10);
        assertEquals(expected, query.export(params));
    }

}