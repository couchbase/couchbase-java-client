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

package com.couchbase.client.java.bucket.api;

import com.couchbase.client.core.message.kv.GetRequest;
import com.couchbase.client.java.document.json.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the functionality of {@link Utils}.
 */
public class UtilsTest {

    @Test
    public void shouldFormatTimeout() {
        GetRequest request = new GetRequest("key", "bucket");

        // {"b":"bucket","s":"kv","t":1000,"i":"0x0"}
        String result = Utils.formatTimeout(request, 1000);

        JsonObject converted = JsonObject.fromJson(result);
        assertEquals("bucket", converted.getString("b"));
        assertEquals("kv", converted.getString("s"));
        assertEquals(1000, (int) converted.getInt("t"));
        assertTrue(converted.getString("i").startsWith("0x"));
        assertFalse(converted.containsKey("c"));
        assertFalse(converted.containsKey("l"));
        assertFalse(converted.containsKey("r"));

        request.lastLocalId("0E637F38FA73001A/FFFFFFFFFA809609");
        request.lastLocalSocket("127.0.0.1:60119");
        request.lastRemoteSocket("127.0.0.1:11210");

        // {"b":"bucket","r":"127.0.0.1:11210","s":"kv","c":"0E637F38FA73001A/FFFFFFFFFA809609",
        //   "t":1000,"i":"0x0","l":"127.0.0.1:60119"}
        result = Utils.formatTimeout(request, 1000);
        converted = JsonObject.fromJson(result);

        assertEquals("0E637F38FA73001A/FFFFFFFFFA809609", converted.getString("c"));
        assertEquals("127.0.0.1:11210", converted.getString("r"));
        assertEquals("127.0.0.1:60119", converted.getString("l"));
    }

}