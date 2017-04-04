/*
 * Copyright (c) 2017 Couchbase, Inc.
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

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Verifies the functionality of {@link GeoDistanceQuery}.
 *
 * @author Michael Nitschinger
 * @since 2.4.5
 */
public class GeoDistanceQueryTest {

    @Test
    public void shouldInjectRequiredParams() {
        GeoDistanceQuery query = new GeoDistanceQuery(1.0, 2.0, "5m");
        JsonObject result = JsonObject.create();
        query.injectParams(result);

        JsonObject expected = JsonObject.create()
            .put("location", JsonArray.from(1.0, 2.0))
            .put("distance", "5m");
        assertEquals(expected, result);
    }

    @Test
    public void shouldInjectField() {
        GeoDistanceQuery query = new GeoDistanceQuery(1.0, 2.0, "5m");
        query.field("fname");
        JsonObject result = JsonObject.create();
        query.injectParams(result);

        JsonObject expected = JsonObject.create()
            .put("location", JsonArray.from(1.0, 2.0))
            .put("distance", "5m")
            .put("field", "fname");
        assertEquals(expected, result);
    }
}
