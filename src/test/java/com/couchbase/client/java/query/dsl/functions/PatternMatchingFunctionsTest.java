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