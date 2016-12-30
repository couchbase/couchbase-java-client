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
package com.couchbase.client.java.transcoder;

import com.couchbase.client.deps.com.fasterxml.jackson.core.Version;
import com.couchbase.client.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.couchbase.client.deps.com.fasterxml.jackson.databind.module.SimpleModule;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the functionality of {@link java.math.BigDecimal} conversion with the system property
 * set.
 *
 * @author Michael Nitschinger
 * @since 2.4.0
 */
public class BigDecimalExactConversionTest {

    public static final ObjectMapper MAPPER = new ObjectMapper();
    public static final SimpleModule JSON_VALUE_MODULE = new SimpleModule("JsonValueModule",
        new Version(1, 0, 0, null, null, null));

    static {
        System.setProperty("com.couchbase.json.decimalForFloat", "true");

        JSON_VALUE_MODULE.addSerializer(JsonObject.class, new JacksonTransformers.JsonObjectSerializer());
        JSON_VALUE_MODULE.addSerializer(JsonArray.class, new JacksonTransformers.JsonArraySerializer());
        JSON_VALUE_MODULE.addDeserializer(JsonObject.class, new JacksonTransformers.JsonObjectDeserializer());
        JSON_VALUE_MODULE.addDeserializer(JsonArray.class, new JacksonTransformers.JsonArrayDeserializer());
        MAPPER.registerModule(JSON_VALUE_MODULE);
    }

    @Test
    public void shouldSupportBigDecimalOnJsonObject() throws Exception {
        BigDecimal bigdec = new BigDecimal("1234.5678901234567890432423432324");
        JsonObject original = JsonObject
            .create()
            .put("value", bigdec);

        String encoded = original.toString();
        assertEquals("{\"value\":1234.5678901234567890432423432324}", encoded);


        JsonObject decoded = MAPPER.readValue(encoded, JsonObject.class);
        assertEquals(bigdec, decoded.getBigDecimal("value"));
        assertEquals(1234.567890123457, decoded.getDouble("value"), 0);
        assertTrue(decoded.getNumber("value") instanceof BigDecimal);
    }

    @Test
    public void shouldSupportBigDecimalOnJsonArray() throws Exception {
        BigDecimal bigdec = new BigDecimal("1234.5678901234567890432423432324");
        JsonArray original = JsonArray.from(bigdec);

        String encoded = original.toString();
        assertEquals("[1234.5678901234567890432423432324]", encoded);

        JsonArray decoded = MAPPER.readValue(encoded, JsonArray.class);
        assertEquals(bigdec, decoded.getBigDecimal(0));
        assertEquals(1234.567890123457, decoded.getDouble(0), 0);
        assertTrue(decoded.getNumber(0) instanceof BigDecimal);
    }

}
