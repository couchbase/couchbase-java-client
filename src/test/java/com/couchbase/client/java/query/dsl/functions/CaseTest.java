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

import com.couchbase.client.java.query.dsl.Expression;
import org.junit.Test;

public class CaseTest {

    @Test
    public void testCaseSimpleDSL() throws Exception {
        Object partial = Case.caseSimple(x("abv"));
        assertFalse(partial instanceof Expression);

        partial = Case.caseSimple(x("abv")).when(x(1));
        assertFalse(partial instanceof Expression);

        partial = Case.caseSimple(x("abv")).when(x(1)).then(x(true));
        assertFalse(partial instanceof Expression);

        partial = Case.caseSimple(x("abv")).when(x(1)).then(x(true)).elseReturn(x(false));
        assertTrue(partial instanceof Expression);
        assertEquals("CASE abv WHEN 1 THEN TRUE ELSE FALSE END", partial.toString());

        partial = Case.caseSimple(x("abv")).when(x(1)).then(x(true)).end();
        assertTrue(partial instanceof Expression);
        assertEquals("CASE abv WHEN 1 THEN TRUE END", partial.toString());
    }

    @Test
    public void testCaseSimpleWithSeveralWhenClauses() {
        Expression caseSimple = Case.caseSimple(x("abv"))
                                    .when(x(10)).then(s("low"))
                                    .when(x(20)).then(s("medium"))
                                    .when(x(30)).then(s("high"))
                .end();

        assertEquals("CASE abv WHEN 10 THEN \"low\" WHEN 20 THEN \"medium\" WHEN 30 THEN \"high\" END", caseSimple.toString());
    }

    @Test
    public void testCaseSearchDSL() throws Exception {
        Object partial = Case.caseSearch();
        assertFalse(partial instanceof Expression);

        partial = Case.caseSearch().when(x("abv").eq(1));
        assertFalse(partial instanceof Expression);

        partial = Case.caseSearch().when(x("abv").eq(1)).then(x(true));
        assertFalse(partial instanceof Expression);

        partial = Case.caseSearch().when(x("abv").eq(1)).then(x(true)).elseReturn(x(false));
        assertTrue(partial instanceof Expression);
        assertEquals("CASE WHEN abv = 1 THEN TRUE ELSE FALSE END", partial.toString());

        partial = Case.caseSearch().when(x("abv").eq(1)).then(x(true)).end();
        assertTrue(partial instanceof Expression);
        assertEquals("CASE WHEN abv = 1 THEN TRUE END", partial.toString());
    }

    @Test
    public void testCaseSearchWithSeveralWhenClauses() {
        Expression caseSimple = Case.caseSearch()
                                    .when(x("abv").lt(10)).then(s("low"))
                                    .when(x("abv").lt(20)).then(s("medium"))
                                    .elseReturn(s("high"));

        assertEquals("CASE WHEN abv < 10 THEN \"low\" WHEN abv < 20 THEN \"medium\" ELSE \"high\" END", caseSimple.toString());
    }
}