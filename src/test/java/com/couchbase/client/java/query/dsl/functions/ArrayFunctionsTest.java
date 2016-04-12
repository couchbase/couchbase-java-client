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

import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;
import static org.junit.Assert.*;

import java.lang.reflect.Array;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.query.dsl.Expression;
import org.junit.Test;

public class ArrayFunctionsTest {

    private static final JsonArray ARRAY = JsonArray.create().add(1).add(true);

    @Test
    public void testArrayAppend() throws Exception {
        Expression e1 = ArrayFunctions.arrayAppend(x(1), x(2));
        Expression e2 = ArrayFunctions.arrayAppend("1", x(2));
        Expression e3 = ArrayFunctions.arrayAppend(ARRAY, x(2));

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ARRAY_APPEND(1, 2)", e1.toString());
        assertEquals("ARRAY_APPEND([1,true], 2)", e3.toString());
    }

    @Test
    public void testArrayAvg() throws Exception {
        Expression e1 = ArrayFunctions.arrayAvg(x(1));
        Expression e2 = ArrayFunctions.arrayAvg("1");
        Expression e3 = ArrayFunctions.arrayAvg(ARRAY);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ARRAY_AVG(1)", e1.toString());
        assertEquals("ARRAY_AVG([1,true])", e3.toString());
    }

    @Test
    public void testArrayConcat() throws Exception {
        Expression e1 = ArrayFunctions.arrayConcat(x(1), x(2));
        Expression e2 = ArrayFunctions.arrayConcat("1", "2");
        Expression e3 = ArrayFunctions.arrayConcat(ARRAY, ARRAY);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ARRAY_CONCAT(1, 2)", e1.toString());
        assertEquals("ARRAY_CONCAT([1,true], [1,true])", e3.toString());
    }

    @Test
    public void testArrayContains() throws Exception {
        Expression e1 = ArrayFunctions.arrayContains(x(1), x(2));
        Expression e2 = ArrayFunctions.arrayContains("1", x(2));
        Expression e3 = ArrayFunctions.arrayContains(ARRAY, x(2));

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ARRAY_CONTAINS(1, 2)", e1.toString());
        assertEquals("ARRAY_CONTAINS([1,true], 2)", e3.toString());
    }

    @Test
    public void testArrayCount() throws Exception {
        Expression e1 = ArrayFunctions.arrayCount(x(1));
        Expression e2 = ArrayFunctions.arrayCount("1");
        Expression e3 = ArrayFunctions.arrayCount(ARRAY);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ARRAY_COUNT(1)", e1.toString());
        assertEquals("ARRAY_COUNT([1,true])", e3.toString());
    }

    @Test
    public void testArrayDistinct() throws Exception {
        Expression e1 = ArrayFunctions.arrayDistinct(x(1));
        Expression e2 = ArrayFunctions.arrayDistinct("1");
        Expression e3 = ArrayFunctions.arrayDistinct(ARRAY);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ARRAY_DISTINCT(1)", e1.toString());
        assertEquals("ARRAY_DISTINCT([1,true])", e3.toString());
    }

    @Test
    public void testArrayIfNull() throws Exception {
        Expression e1 = ArrayFunctions.arrayIfNull(x(1));
        Expression e2 = ArrayFunctions.arrayIfNull("1");
        Expression e3 = ArrayFunctions.arrayIfNull(ARRAY);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ARRAY_IFNULL(1)", e1.toString());
        assertEquals("ARRAY_IFNULL([1,true])", e3.toString());
    }

    @Test
    public void testArrayLength() throws Exception {
        Expression e1 = ArrayFunctions.arrayLength(x(1));
        Expression e2 = ArrayFunctions.arrayLength("1");
        Expression e3 = ArrayFunctions.arrayLength(ARRAY);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ARRAY_LENGTH(1)", e1.toString());
        assertEquals("ARRAY_LENGTH([1,true])", e3.toString());
    }

    @Test
    public void testArrayMax() throws Exception {
        Expression e1 = ArrayFunctions.arrayMax(x(1));
        Expression e2 = ArrayFunctions.arrayMax("1");
        Expression e3 = ArrayFunctions.arrayMax(ARRAY);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ARRAY_MAX(1)", e1.toString());
        assertEquals("ARRAY_MAX([1,true])", e3.toString());
    }

    @Test
    public void testArrayMin() throws Exception {
        Expression e1 = ArrayFunctions.arrayMin(x(1));
        Expression e2 = ArrayFunctions.arrayMin("1");
        Expression e3 = ArrayFunctions.arrayMin(ARRAY);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ARRAY_MIN(1)", e1.toString());
        assertEquals("ARRAY_MIN([1,true])", e3.toString());
    }

    @Test
    public void testArrayPosition() throws Exception {
        Expression e1 = ArrayFunctions.arrayPosition(x(1), x(2));
        Expression e2 = ArrayFunctions.arrayPosition("1", x(2));
        Expression e3 = ArrayFunctions.arrayPosition(ARRAY, x(2));

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ARRAY_POSITION(1, 2)", e1.toString());
        assertEquals("ARRAY_POSITION([1,true], 2)", e3.toString());
    }

    @Test
    public void testArrayPrepend() throws Exception {
        Expression e1 = ArrayFunctions.arrayPrepend(x(1), x(2));
        Expression e2 = ArrayFunctions.arrayPrepend("1", x(2));
        Expression e3 = ArrayFunctions.arrayPrepend(ARRAY, x(2));

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ARRAY_PREPEND(2, 1)", e1.toString());
        assertEquals("ARRAY_PREPEND(2, [1,true])", e3.toString());
    }

    @Test
    public void testArrayPut() throws Exception {
        Expression e1 = ArrayFunctions.arrayPut(x(1), x(2));
        Expression e2 = ArrayFunctions.arrayPut("1", x(2));
        Expression e3 = ArrayFunctions.arrayPut(ARRAY, x(2));

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ARRAY_PUT(1, 2)", e1.toString());
        assertEquals("ARRAY_PUT([1,true], 2)", e3.toString());
    }

    @Test
    public void testArrayRange() throws Exception {
        Expression range = ArrayFunctions.arrayRange(1, 101);
        assertEquals("ARRAY_RANGE(1, 101)", range.toString());
    }

    @Test
    public void testArrayRangeWithStep() throws Exception {
        Expression range = ArrayFunctions.arrayRange(1, 101, 10);
        assertEquals("ARRAY_RANGE(1, 101, 10)", range.toString());
    }

    @Test
    public void testArrayRemove() throws Exception {
        Expression e1 = ArrayFunctions.arrayRemove(x(1), x(2));
        Expression e2 = ArrayFunctions.arrayRemove("1", x(2));
        Expression e3 = ArrayFunctions.arrayRemove(ARRAY, x(2));

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ARRAY_REMOVE(1, 2)", e1.toString());
        assertEquals("ARRAY_REMOVE([1,true], 2)", e3.toString());
    }

    @Test
    public void testArrayRepeat() throws Exception {
        Expression e1 = ArrayFunctions.arrayRepeat(x(1.2f), 5);
        Expression e2 = ArrayFunctions.arrayRepeat(1.2f, 5);
        Expression e3 = ArrayFunctions.arrayRepeat(1, 5);
        Expression e4 = ArrayFunctions.arrayRepeat("1", 5);
        Expression e5 = ArrayFunctions.arrayRepeat(true, 5);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ARRAY_REPEAT(1.2, 5)", e1.toString());
        assertEquals("ARRAY_REPEAT(1, 5)", e3.toString());
        assertEquals("ARRAY_REPEAT(\"1\", 5)", e4.toString());
        assertEquals("ARRAY_REPEAT(TRUE, 5)", e5.toString());
    }

    @Test
    public void testArrayReplace() throws Exception {
        Expression e1 = ArrayFunctions.arrayReplace(x(1), x(1), s("b"));
        Expression e2 = ArrayFunctions.arrayReplace("1", x(1), s("b"));
        Expression e3 = ArrayFunctions.arrayReplace(ARRAY, x(1), s("b"));

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ARRAY_REPLACE(1, 1, \"b\")", e1.toString());
        assertEquals("ARRAY_REPLACE([1,true], 1, \"b\")", e3.toString());
    }

    @Test
    public void testArrayReplaceAtMost() throws Exception {
        Expression e1 = ArrayFunctions.arrayReplace(x(1), x(1), s("b"), 2);
        Expression e2 = ArrayFunctions.arrayReplace("1", x(1), s("b"), 2);
        Expression e3 = ArrayFunctions.arrayReplace(ARRAY, x(1), s("b"), 2);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ARRAY_REPLACE(1, 1, \"b\", 2)", e1.toString());
        assertEquals("ARRAY_REPLACE([1,true], 1, \"b\", 2)", e3.toString());
    }

    @Test
    public void testArrayReverse() throws Exception {
        Expression e1 = ArrayFunctions.arrayReverse(x(1));
        Expression e2 = ArrayFunctions.arrayReverse("1");
        Expression e3 = ArrayFunctions.arrayReverse(ARRAY);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ARRAY_REVERSE(1)", e1.toString());
        assertEquals("ARRAY_REVERSE([1,true])", e3.toString());
    }

    @Test
    public void testArraySort() throws Exception {
        Expression e1 = ArrayFunctions.arraySort(x(1));
        Expression e2 = ArrayFunctions.arraySort("1");
        Expression e3 = ArrayFunctions.arraySort(ARRAY);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ARRAY_SORT(1)", e1.toString());
        assertEquals("ARRAY_SORT([1,true])", e3.toString());
    }

    @Test
    public void testArraySum() throws Exception {
        Expression e1 = ArrayFunctions.arraySum(x(1));
        Expression e2 = ArrayFunctions.arraySum("1" );
        Expression e3 = ArrayFunctions.arraySum(ARRAY);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ARRAY_SUM(1)", e1.toString());
        assertEquals("ARRAY_SUM([1,true])", e3.toString());
    }
}