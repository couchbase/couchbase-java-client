/*
 * Copyright (c) 2018 Couchbase, Inc.
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
package com.couchbase.client.java.analytics;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Verifies the functionality of the {@link ParameterizedAnalyticsQuery}.
 *
 * @since 2.6.2
 */
public class ParameterizedAnalyticsQueryTest {

    @Test
    public void shouldPassNamedParams() {
        JsonObject named = JsonObject.create()
                .put("num", 1)
                .put("$b", "foobar");

        ParameterizedAnalyticsQuery query = new ParameterizedAnalyticsQuery(
                "select 1=num where a=$b",
                null,
                named,
                null
        );

        JsonObject expected = JsonObject.fromJson
                ("{\"$num\":1,\"statement\":\"select 1=num where a=$b\",\"$b\":\"foobar\"}");
        JsonObject result = query.query();
        assertEquals(result, expected);
    }

    @Test
    public void shouldPassPositionalParams() {
        JsonArray positional = JsonArray.from(1, "foo", true);

        ParameterizedAnalyticsQuery query = new ParameterizedAnalyticsQuery(
                "select 1=? where a=? or b=?",
                positional,
                null,
                null
        );

        JsonObject expected = JsonObject.fromJson
                ("{\"args\":[1,\"foo\",true],\"statement\":\"select 1=? where a=? or b=?\"}");
        JsonObject result = query.query();
        assertEquals(result, expected);
    }

}