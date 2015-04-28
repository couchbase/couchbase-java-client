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

public class ComparisonFunctionsTest {

    @Test
    public void testGreatest() throws Exception {
        Expression greatest = ComparisonFunctions.greatest(x(1), x(2));
        assertEquals("GREATEST(1, 2)", greatest.toString());
    }

    @Test
    public void testGreatestWithVarargs() throws Exception {
        Expression greatest = ComparisonFunctions.greatest(x(1), x(2), x(3), null, x(5));
        assertEquals("GREATEST(1, 2, 3, NULL, 5)", greatest.toString());

        greatest = ComparisonFunctions.greatest(x(1), x(2), (Expression) null);
        assertEquals("GREATEST(1, 2, NULL)", greatest.toString());

        greatest = ComparisonFunctions.greatest(x(1), x(2), (Expression[]) null);
        assertEquals("GREATEST(1, 2)", greatest.toString());
    }

    @Test
    public void testLeast() throws Exception {
        Expression least = ComparisonFunctions.least(x(1), x(2));
        assertEquals("LEAST(1, 2)", least.toString());
    }

    @Test
    public void testLeastWithVarargs() throws Exception {
        Expression least = ComparisonFunctions.least(x(1), x(2), x(3), null, x(5));
        assertEquals("LEAST(1, 2, 3, NULL, 5)", least.toString());

        least = ComparisonFunctions.least(x(1), x(2), (Expression) null);
        assertEquals("LEAST(1, 2, NULL)", least.toString());

        least = ComparisonFunctions.least(x(1), x(2), (Expression[]) null);
        assertEquals("LEAST(1, 2)", least.toString());
    }
}