/**
 * Copyright (C) 2014 Couchbase, Inc.
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
package com.couchbase.client.java.query.dsl;

import static com.couchbase.client.java.query.dsl.Expression.i;
import static org.junit.Assert.assertEquals;

import com.couchbase.client.java.query.Select;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.dsl.path.AsPath;
import org.junit.Test;

public class ExpressionTest {

    @Test
    public void shouldEscapeOneIdentifier() {
        Expression escaped = i("beer-sample");
        assertEquals("`beer-sample`", escaped.toString());
    }

    @Test
    public void shouldEscapedMultipleIdentifiers() {
        Expression escaped = i("beer-sample", "someothersample", "third-sample");
        assertEquals("`beer-sample`, `someothersample`, `third-sample`", escaped.toString());
    }

    @Test
    public void shouldEscapeIdentifierInFromClause() {
        Statement escapedFrom = Select.select("*").from(i("test"));
        assertEquals("SELECT * FROM `test`", escapedFrom.toString());
    }

}