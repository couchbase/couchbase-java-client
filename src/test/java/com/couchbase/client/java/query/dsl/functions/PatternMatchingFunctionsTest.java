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
import static com.couchbase.client.java.query.dsl.functions.PatternMatchingFunctions.regexpContains;
import static com.couchbase.client.java.query.dsl.functions.PatternMatchingFunctions.regexpLike;
import static com.couchbase.client.java.query.dsl.functions.PatternMatchingFunctions.regexpPosition;
import static com.couchbase.client.java.query.dsl.functions.PatternMatchingFunctions.regexpReplace;
import static org.junit.Assert.*;

import com.couchbase.client.java.query.dsl.Expression;
import org.junit.Test;

public class PatternMatchingFunctionsTest {

    @Test
    public void testRegexpContains() throws Exception {
        Expression e1 = regexpContains(x("stringField"), "pattern");
        Expression e2 = regexpContains("stringField", "pattern");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("REGEXP_CONTAINS(stringField, \"pattern\")", e1.toString());
    }

    @Test
    public void testRegexpLike() throws Exception {
        Expression e1 = regexpLike(x("stringField"), "pattern");
        Expression e2 = regexpLike("stringField", "pattern");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("REGEXP_LIKE(stringField, \"pattern\")", e1.toString());
    }

    @Test
    public void testRegexpPosition() throws Exception {
        Expression e1 = regexpPosition(x("stringField"), "pattern");
        Expression e2 = regexpPosition("stringField", "pattern");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("REGEXP_POSITION(stringField, \"pattern\")", e1.toString());
    }

    @Test
    public void testRegexpReplaceAll() throws Exception {
        Expression e1 = regexpReplace(x("stringField"), "pattern", "all");
        Expression e2 = regexpReplace("stringField", "pattern", "all");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("REGEXP_REPLACE(stringField, \"pattern\", \"all\")", e1.toString());
    }

    @Test
    public void testRegexpReplaceN() throws Exception {
        Expression e1 = regexpReplace(x("stringField"), "pattern", "replace", 3);
        Expression e2 = regexpReplace("stringField", "pattern", "replace", 3);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("REGEXP_REPLACE(stringField, \"pattern\", \"replace\", 3)", e1.toString());
    }
}