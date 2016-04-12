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