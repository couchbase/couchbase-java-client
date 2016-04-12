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