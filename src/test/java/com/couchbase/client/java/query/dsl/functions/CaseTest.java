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