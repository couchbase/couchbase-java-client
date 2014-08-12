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
package com.couchbase.client.java.document.json;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JsonObjectTest {

    @Test
    public void shouldExportEmptyObject() {
        String result = JsonObject.empty().toString();
        assertEquals("{}", result);
    }

    @Test
    public void shouldExportStrings() {
        String result = JsonObject.empty().put("key", "value").toString();
        assertEquals("{\"key\":\"value\"}", result);
    }

    @Test
    public void shouldExportMix() {
        JsonObject obj = JsonObject.empty()
            .put("name", "michael")
            .put("age", 25)
            .put("developer", true);
        assertEquals("{\"name\":\"michael\",\"developer\":true,\"age\":25}", obj.toString());
    }

    @Test
    public void shouldExportNestedObjects() {
        JsonObject obj = JsonObject.empty()
            .put("nested", JsonObject.empty().put("a", true));
        assertEquals("{\"nested\":{\"a\":true}}", obj.toString());
    }

    @Test
    public void shouldExportNestedArrays() {
        JsonObject obj = JsonObject.empty()
            .put("nested", JsonArray.empty().add(true).add(4).add("foo"));
        assertEquals("{\"nested\":[true,4,\"foo\"]}", obj.toString());
    }

    @Test
    public void shouldReturnNullWhenNotFound() {
        JsonObject obj = JsonObject.empty();
        assertNull(obj.getInt("notfound"));
    }

}