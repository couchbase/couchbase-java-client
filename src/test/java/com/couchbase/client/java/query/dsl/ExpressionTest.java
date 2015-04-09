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

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.Select;
import com.couchbase.client.java.query.Statement;
import org.junit.Test;

import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;
import static org.junit.Assert.assertEquals;

public class ExpressionTest {

    @Test
    public void shouldCreateRawExpression() {
        Expression expr = x("foobar");
        assertEquals("foobar", expr.toString());

        expr = x(42);
        assertEquals("42", expr.toString());

        expr = x(Long.MAX_VALUE);
        assertEquals("9223372036854775807", expr.toString());

        expr = x(42.143);
        assertEquals("42.143", expr.toString());

        expr = x(Double.MAX_VALUE);
        assertEquals("1.7976931348623157E308", expr.toString());

        expr = x(true);
        assertEquals("TRUE", expr.toString());

        expr = x(false);
        assertEquals("FALSE", expr.toString());

        expr = x(JsonObject.create().put("foo", "bar"));
        assertEquals("{\"foo\":\"bar\"}", expr.toString());

        expr = x(JsonArray.create().add(true).add(123).add("hello"));
        assertEquals("[true,123,\"hello\"]", expr.toString());
    }

    @Test
    public void shouldCreateLiteralExpressions() {
        assertEquals("NULL", Expression.NULL().toString());
        assertEquals("FALSE", Expression.FALSE().toString());
        assertEquals("TRUE", Expression.TRUE().toString());
        assertEquals("MISSING", Expression.MISSING().toString());
    }

    @Test
    public void shouldEscapeIdentifiers() {
        Expression escaped = i("beer-sample");
        assertEquals("`beer-sample`", escaped.toString());

        escaped = i("beer-sample", "someothersample", "third-sample");
        assertEquals("`beer-sample`, `someothersample`, `third-sample`", escaped.toString());
    }

    @Test
    public void shouldWrapWithStringQuotes() {
        Expression quoted = s("foobar");
        assertEquals("\"foobar\"", quoted.toString());

        quoted = s("foo", "bar");
        assertEquals("\"foo\", \"bar\"", quoted.toString());
    }

    @Test
    public void shouldEscapeIdentifierInFromClause() {
        Statement escapedFrom = Select.select("*").from(i("test"));
        assertEquals("SELECT * FROM `test`", escapedFrom.toString());
    }

    @Test
    public void shouldNegateExpression() {
        Expression expr = x("foobar").not();
        assertEquals("NOT foobar", expr.toString());
    }

    @Test
    public void shouldAndOrCombineExpressions() {
        Expression expr = x("foo").and(x("bar"));
        assertEquals("foo AND bar", expr.toString());

        expr = i("blu-rb").or(x(5));
        assertEquals("`blu-rb` OR 5", expr.toString());
    }

    @Test
    public void shouldCombineExpressionsWithArithmetic() {
        Expression expr = x(5).eq(x(5));
        assertEquals("5 = 5", expr.toString());

        expr = s("foo").ne(s("bar"));
        assertEquals("\"foo\" != \"bar\"", expr.toString());

        expr = x(6).gt(3);
        assertEquals("6 > 3", expr.toString());

        expr = x(3).lt(8);
        assertEquals("3 < 8", expr.toString());

        expr = x(3).gte(3);
        assertEquals("3 >= 3", expr.toString());

        expr = x(8).lte(8);
        assertEquals("8 <= 8", expr.toString());

        expr = x("foo").concat(x("bar"));
        assertEquals("foo || bar", expr.toString());
    }

    @Test
    public void shouldCombineIsExpressions() {
        Expression expr = x("foo").isValued();
        assertEquals("foo IS VALUED", expr.toString());

        expr = x("foo").isNotValued();
        assertEquals("foo IS NOT VALUED", expr.toString());

        expr = x("foo").isNull();
        assertEquals("foo IS NULL", expr.toString());

        expr = x("foo").isNotNull();
        assertEquals("foo IS NOT NULL", expr.toString());

        expr = x("foo").isMissing();
        assertEquals("foo IS MISSING", expr.toString());

        expr = x("foo").isNotMissing();
        assertEquals("foo IS NOT MISSING", expr.toString());
    }

    @Test
    public void shouldCombineVariousOperators() {
        Expression expr = x(5).between(3).and(5);
        assertEquals("5 BETWEEN 3 AND 5", expr.toString());

        expr = x(8).notBetween(x(3)).and(x(5));
        assertEquals("8 NOT BETWEEN 3 AND 5", expr.toString());

        expr = i("firstname").like(s("michael%"));
        assertEquals("`firstname` LIKE \"michael%\"", expr.toString());

        expr = i("firstname").notLike(s("michael%"));
        assertEquals("`firstname` NOT LIKE \"michael%\"", expr.toString());

        expr = x("foo").exists();
        assertEquals("EXISTS foo", expr.toString());

        expr = x("firstname").in(x(JsonArray.from("a", "b", "c")));
        assertEquals("firstname IN [\"a\",\"b\",\"c\"]", expr.toString());

        expr = x("firstname").notIn(x(JsonArray.from("a", "b", "c")));
        assertEquals("firstname NOT IN [\"a\",\"b\",\"c\"]", expr.toString());

        expr = x("firstname").as("fname");
        assertEquals("firstname AS fname", expr.toString());
    }

}