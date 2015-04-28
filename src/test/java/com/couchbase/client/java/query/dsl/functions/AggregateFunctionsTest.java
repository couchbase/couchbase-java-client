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

public class AggregateFunctionsTest {

    @Test
    public void testArrayAgg() throws Exception {
        Expression e1 = AggregateFunctions.arrayAgg(x(1));
        Expression e2 = AggregateFunctions.arrayAgg("1");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ARRAY_AGG(1)", e1.toString());
    }

    @Test
    public void testAvg() throws Exception {
        Expression e1 = AggregateFunctions.avg(x(1));
        Expression e2 = AggregateFunctions.avg("1");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("AVG(1)", e1.toString());
    }

    @Test
    public void testCount() throws Exception {
        Expression e1 = AggregateFunctions.count(x(1));
        Expression e2 = AggregateFunctions.count("1");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("COUNT(1)", e1.toString());
    }

    @Test
    public void testCountAll() throws Exception {
        assertEquals("COUNT(*)", AggregateFunctions.countAll().toString());
    }

    @Test
    public void testMax() throws Exception {
        Expression e1 = AggregateFunctions.max(x(1));
        Expression e2 = AggregateFunctions.max("1");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("MAX(1)", e1.toString());
    }

    @Test
    public void testMin() throws Exception {
        Expression e1 = AggregateFunctions.min(x(1));
        Expression e2 = AggregateFunctions.min("1");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("MIN(1)", e1.toString());
    }

    @Test
    public void testSum() throws Exception {
        Expression e1 = AggregateFunctions.sum(x(1));
        Expression e2 = AggregateFunctions.sum("1");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("SUM(1)", e1.toString());
    }

    @Test
    public void testDistinct() throws Exception {
        Expression e1 = AggregateFunctions.distinct(x(1));
        Expression e2 = AggregateFunctions.distinct("1");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("DISTINCT 1", e1.toString());
    }
}