/**
 * Copyright (C) 2014 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */
package com.couchbase.client.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import com.couchbase.client.java.document.JsonArrayDocument;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.document.json.JsonValue;
import com.couchbase.client.java.util.ClusterDependentTest;
import org.junit.Test;

/**
 * Tests that relate to JsonDocuments and conversions that can occur in the underlying JsonObject.
 *
 * @author Simon Baslé
 * @since 2.0.2
 */
public class JsonDocumentTest extends ClusterDependentTest {

    /**
     * This test checks against ClassCastExceptions when
     * storing/getting Numbers in a JsonObject.
     */
    @Test
    public void testNumberConversionInJsonObject() {
        //actually storing the document and retrieving it can lead to type conversion
        //because underneath Jackson detects eg the number is small enough to be just an int
        JsonObject jsonObject = JsonObject.empty().put("value", 0L);
        bucket().insert(JsonDocument.create("jsonObjectTestDocument", jsonObject));
        JsonDocument convertedDoc = bucket().get("jsonObjectTestDocument");
        JsonObject value = convertedDoc.content();

        //check no ClassCastExceptions occurs
        assertThat(value.getLong("value"), notNullValue());
        assertThat(value.getDouble("value"), notNullValue());
        assertThat(value.getInt("value"), notNullValue());
    }

    /**
     * This test checks against ClassCastExceptions when
     * storing/getting Numbers in a JsonArray.
     */
    @Test
    public void testNumberConversionInJsonArray() {
        //actually storing the document and retrieving it can lead to type conversion
        //because underneath Jackson detects eg the number is small enough to be just an int
        JsonArray jsonArray = JsonArray.empty().add(0L);
        bucket().insert(JsonArrayDocument.create("jsonArrayTestDocument", jsonArray));
        JsonArrayDocument convertedDoc = bucket().get("jsonArrayTestDocument", JsonArrayDocument.class);
        JsonArray value = convertedDoc.content();

        //check no ClassCastExceptions occurs
        assertThat(value.getLong(0), notNullValue());
        assertThat(value.getDouble(0), notNullValue());
        assertThat(value.getInt(0), notNullValue());
    }

    @Test
    public void shouldPersistAndLoadWithNullValues() {
        final String key = "testPersistAndLoadWithNullValues";

        JsonObject obj = JsonObject.create()
            .put("name", "test")
            .put("age", 21)
            .put("role", (Object) null)
            .put("hobby", JsonValue.NULL)
            .putNull("superior");

        JsonDocument doc = JsonDocument.create(key, obj);
        bucket().upsert(doc);

        JsonDocument stored = bucket().get(key);
        assertThat(stored, notNullValue());
        assertThat(stored.content(), notNullValue());
        assertThat(stored.content().get("name"), instanceOf(String.class));
        assertThat(stored.content().get("age"), instanceOf(Number.class));
        assertThat(stored.content().containsKey("role"), is(true));
        assertThat(stored.content().containsKey("superior"), is(true));
        assertThat(stored.content().containsKey("hobby"), is(true));
        assertThat(stored.content().get("hobby"), nullValue());
        assertThat(stored.content().get("role"), nullValue());
        assertThat(stored.content().get("superior"), nullValue());
    }
}
