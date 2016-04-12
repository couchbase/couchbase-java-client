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