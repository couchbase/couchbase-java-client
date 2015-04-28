/*
 * Copyright (C) 2015 Couchbase, Inc.
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

package com.couchbase.client.java.query.dsl.functions;

import static com.couchbase.client.java.query.dsl.Expression.x;
import static com.couchbase.client.java.query.dsl.functions.JsonFunctions.decodeJson;
import static com.couchbase.client.java.query.dsl.functions.JsonFunctions.encodeJson;
import static com.couchbase.client.java.query.dsl.functions.JsonFunctions.encodedSize;
import static com.couchbase.client.java.query.dsl.functions.JsonFunctions.polyLength;
import static org.junit.Assert.*;

import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.dsl.Expression;
import org.junit.Test;

public class JsonFunctionsTest {

    @Test
    public void testDecodeJson() throws Exception {
        Expression e1 = decodeJson(x("jsonContainingField"));
        Expression e2 = decodeJson("{\"test\": true}");
        Expression e3 = decodeJson(JsonObject.create().put("test", true));

        assertEquals("DECODE_JSON(jsonContainingField)", e1.toString());
        assertEquals("DECODE_JSON(\"{\\\"test\\\":true}\")", e2.toString());
        assertEquals("DECODE_JSON(\"{\\\"test\\\":true}\")", e3.toString());
    }

    @Test
    public void testEncodeJson() throws Exception {
        Expression e1 = encodeJson(x(1));
        Expression e2 = encodeJson("1");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ENCODE_JSON(1)", e1.toString());
    }

    @Test
    public void testEncodedSize() throws Exception {
        Expression e1 = encodedSize(x(1));
        Expression e2 = encodedSize("1");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ENCODED_SIZE(1)", e1.toString());
    }

    @Test
    public void testPolyLength() throws Exception {
        Expression e1 = polyLength(x(1));
        Expression e2 = polyLength("1");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("POLY_LENGTH(1)", e1.toString());
    }
}