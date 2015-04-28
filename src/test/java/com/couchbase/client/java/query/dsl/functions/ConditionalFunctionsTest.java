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
import static org.junit.Assert.*;

import com.couchbase.client.java.query.dsl.Expression;
import org.junit.Test;

public class ConditionalFunctionsTest {

    @Test
    public void testBuild() throws Exception {
        Expression withMinimum = ConditionalFunctions.build("TEST", x(1), x(2));
        Expression withNullOthers = ConditionalFunctions.build("TEST", x(1), x(2), (Expression[]) null);
        Expression withManyOthers = ConditionalFunctions.build("TEST", x(1), x(2), x(3), x(4));
        Expression withOneOtherNull = ConditionalFunctions.build("TEST", x(1), x(2), (Expression) null);
        Expression withManyOthersWithNull = ConditionalFunctions.build("TEST", x(1), x(2), x(3), null, x(5));

        assertEquals("TEST(1, 2)", withMinimum.toString());
        assertEquals("TEST(1, 2)", withNullOthers.toString());
        assertEquals("TEST(1, 2, 3, 4)", withManyOthers.toString());
        assertEquals("TEST(1, 2, NULL)", withOneOtherNull.toString());
        assertEquals("TEST(1, 2, 3, NULL, 5)", withManyOthersWithNull.toString());
    }

    @Test
    public void testIfMissing() throws Exception {
        assertEquals("IFMISSING(1, 2, 3)", ConditionalFunctions.ifMissing(x(1), x(2), x(3)).toString());
    }

    @Test
    public void testIfMissingOrNull() throws Exception {
        assertEquals("IFMISSINGORNULL(1, 2, 3)", ConditionalFunctions.ifMissingOrNull(x(1), x(2), x(3)).toString());
    }

    @Test
    public void testIfNull() throws Exception {
        assertEquals("IFNULL(1, 2, 3)", ConditionalFunctions.ifNull(x(1), x(2), x(3)).toString());
    }

    @Test
    public void testMissingIf() throws Exception {
        assertEquals("MISSINGIF(1, 2)", ConditionalFunctions.missingIf(x(1), x(2)).toString());
    }

    @Test
    public void testNullIf() throws Exception {
        assertEquals("NULLIF(1, 2)", ConditionalFunctions.nullIf(x(1), x(2)).toString());
    }

    @Test
    public void testIfInf() throws Exception {
        assertEquals("IFINF(1, 2, 3)", ConditionalFunctions.ifInf(x(1), x(2), x(3)).toString());
    }

    @Test
    public void testIfNaN() throws Exception {
        assertEquals("IFNAN(1, 2, 3)", ConditionalFunctions.ifNaN(x(1), x(2), x(3)).toString());
    }

    @Test
    public void testIfNaNOrInf() throws Exception {
        assertEquals("IFNANORINF(1, 2, 3)", ConditionalFunctions.ifNaNOrInf(x(1), x(2), x(3)).toString());
    }

    @Test
    public void testNanIf() throws Exception {
        assertEquals("NANIF(1, 2)", ConditionalFunctions.nanIf(x(1), x(2)).toString());
    }

    @Test
    public void testNegInfIf() throws Exception {
        assertEquals("NEGINFIF(1, 2)", ConditionalFunctions.negInfIf(x(1), x(2)).toString());
    }

    @Test
    public void testPosInfIf() throws Exception {
        assertEquals("POSINFIF(1, 2)", ConditionalFunctions.posInfIf(x(1), x(2)).toString());
    }
}