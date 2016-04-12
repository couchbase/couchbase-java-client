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

package com.couchbase.client.java.query.dsl.functions;

import static com.couchbase.client.java.query.dsl.Expression.x;
import static com.couchbase.client.java.query.dsl.functions.StringFunctions.contains;
import static com.couchbase.client.java.query.dsl.functions.StringFunctions.initCap;
import static com.couchbase.client.java.query.dsl.functions.StringFunctions.length;
import static com.couchbase.client.java.query.dsl.functions.StringFunctions.lower;
import static com.couchbase.client.java.query.dsl.functions.StringFunctions.ltrim;
import static com.couchbase.client.java.query.dsl.functions.StringFunctions.position;
import static com.couchbase.client.java.query.dsl.functions.StringFunctions.repeat;
import static com.couchbase.client.java.query.dsl.functions.StringFunctions.replace;
import static com.couchbase.client.java.query.dsl.functions.StringFunctions.rtrim;
import static com.couchbase.client.java.query.dsl.functions.StringFunctions.split;
import static com.couchbase.client.java.query.dsl.functions.StringFunctions.substr;
import static com.couchbase.client.java.query.dsl.functions.StringFunctions.title;
import static com.couchbase.client.java.query.dsl.functions.StringFunctions.trim;
import static com.couchbase.client.java.query.dsl.functions.StringFunctions.upper;
import static org.junit.Assert.*;

import com.couchbase.client.java.query.dsl.Expression;
import org.junit.Test;

public class StringFunctionsTest {

    @Test
    public void testContains() throws Exception {
        Expression e1 = contains(x("stringField"), "value");
        Expression e2 = contains("stringField", "value");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("CONTAINS(stringField, \"value\")", e1.toString());
    }

    @Test
    public void testInitCap() throws Exception {
        Expression e1 = initCap(x("stringField"));
        Expression e2 = initCap("stringField");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("INITCAP(stringField)", e1.toString());
    }

    @Test
    public void testTitle() throws Exception {
        Expression e1 = title(x("stringField"));
        Expression e2 = title("stringField");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("TITLE(stringField)", e1.toString());
    }

    @Test
    public void testLength() throws Exception {
        Expression e1 = length(x("stringField"));
        Expression e2 = length("stringField");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("LENGTH(stringField)", e1.toString());
    }

    @Test
    public void testLower() throws Exception {
        Expression e1 = lower(x("stringField"));
        Expression e2 = lower("stringField");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("LOWER(stringField)", e1.toString());
    }

    @Test
    public void testLtrimWhitespaces() throws Exception {
        Expression e1 = ltrim(x("stringField"));
        Expression e2 = ltrim("stringField");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("LTRIM(stringField)", e1.toString());
    }

    @Test
    public void testLtrimOthers() throws Exception {
        Expression e1 = ltrim(x("stringField"), "abc");
        Expression e2 = ltrim("stringField", "abc");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("LTRIM(stringField, \"abc\")", e1.toString());
    }

    @Test
    public void testPosition() throws Exception {
        Expression e1 = position(x("stringField"), "value");
        Expression e2 = position("stringField", "value");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("POSITION(stringField, \"value\")", e1.toString());
    }

    @Test
    public void testRepeat() throws Exception {
        Expression e1 = repeat(x("stringField"), 5);
        Expression e2 = repeat("stringField", 5);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("REPEAT(stringField, 5)", e1.toString());
    }

    @Test
    public void testReplaceAll() throws Exception {
        Expression e1 = replace(x("stringField"), "value", "replaced");
        Expression e2 = replace("stringField", "value", "replaced");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("REPLACE(stringField, \"value\", \"replaced\")", e1.toString());
    }

    @Test
    public void testReplaceN() throws Exception {
        Expression e1 = replace(x("stringField"), "value", "replaced", 5);
        Expression e2 = replace("stringField", "value", "replaced", 5);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("REPLACE(stringField, \"value\", \"replaced\", 5)", e1.toString());

    }

    @Test
    public void testRtrimWhitespaces() throws Exception {
        Expression e1 = rtrim(x("stringField"));
        Expression e2 = rtrim("stringField");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("RTRIM(stringField)", e1.toString());
    }

    @Test
    public void testRtrimOthers() throws Exception {
        Expression e1 = rtrim(x("stringField"), "abc");
        Expression e2 = rtrim("stringField", "abc");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("RTRIM(stringField, \"abc\")", e1.toString());
    }

    @Test
    public void testSplitWhitespaces() throws Exception {
        Expression e1 = split(x("stringField"));
        Expression e2 = split("stringField");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("SPLIT(stringField)", e1.toString());
    }

    @Test
    public void testSplitOthers() throws Exception {
        Expression e1 = split(x("stringField"), "abc");
        Expression e2 = split("stringField", "abc");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("SPLIT(stringField, \"abc\")", e1.toString());
    }

    @Test
    public void testSubstrToEnd() throws Exception {
        Expression e1 = substr(x("stringField"), 3);
        Expression e2 = substr("stringField", 3);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("SUBSTR(stringField, 3)", e1.toString());
    }

    @Test
    public void testSubstrToNLength() throws Exception {
        Expression e1 = substr(x("stringField"), -1, 3);
        Expression e2 = substr("stringField", -1, 3);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("SUBSTR(stringField, -1, 3)", e1.toString());
    }

    @Test
    public void testTrimWhitespaces() throws Exception {
        Expression e1 = trim(x("stringField"));
        Expression e2 = trim("stringField");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("TRIM(stringField)", e1.toString());
    }

    @Test
    public void testTrimOthers() throws Exception {
        Expression e1 = trim(x("stringField"), "abc");
        Expression e2 = trim("stringField", "abc");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("TRIM(stringField, \"abc\")", e1.toString());
    }

    @Test
    public void testUpper() throws Exception {
        Expression e1 = upper(x("stringField"));
        Expression e2 = upper("stringField");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("UPPER(stringField)", e1.toString());
    }
}