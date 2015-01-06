/*
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

package com.couchbase.client.java.query;

import static com.couchbase.client.java.query.Select.select;
import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;
import static org.junit.Assert.assertEquals;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import org.junit.Test;

/**
 * Verifies various combinations of N1QL queries and their transformation to N1QL string.
 *
 * @author Simon Baslé
 * @since 2.1
 */
public class QueryToN1qlTest {

    @Test
    public void simpleQueryShouldJustProduceStatement() {
        SimpleQuery query = new SimpleQuery(select("*").from("tutorial").where(x("fname").eq(s("ian"))));

        //notice ian is between escaped quotes since inside json
        assertEquals("{\"statement\":\"SELECT * FROM tutorial WHERE fname = \\\"ian\\\"\"}", query.toN1QL());
    }

    @Test
    public void parametrizedQueryWithArrayShouldProduceStatementAndArgs() {
        ParametrizedQuery query = new ParametrizedQuery(select("*"), JsonArray.from("aString", 123, true));

        JsonObject expected = JsonObject.create()
            .put("statement", "SELECT *")
            .put("args", JsonArray.from("aString", 123, true));

        assertEquals(expected.toString(), query.toN1QL());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRefuseToPrepareQueryPlan() {
        QueryPlan fakePlan = new QueryPlan(JsonObject.empty());
        PrepareStatement.prepare(fakePlan);
    }

    @Test
    public void shouldNotWrapAPrepareStatement() {
        PrepareStatement statement = PrepareStatement.prepare(select("*"));
        assertEquals(statement, PrepareStatement.prepare(statement));
    }

    @Test
    public void shouldPrependStatementWithPrepare() {
        Statement toPrepare = select("*").from("default");
        PrepareStatement prepare = PrepareStatement.prepare(toPrepare);

        assertEquals("PREPARE SELECT * FROM default", prepare.toString());
    }

    @Test
    public void preparedQueryWithArrayShouldProducePreparedAndArgs() {
        JsonObject rawPlan = JsonObject.create().put("fake", "select *");
        QueryPlan fakePlan = new QueryPlan(rawPlan);
        JsonArray params =JsonArray.from("aString", 123, true);
        PreparedQuery query = new PreparedQuery(fakePlan, params);

        JsonObject expected = JsonObject.create()
            .put("prepared", rawPlan)
            .put("args", JsonArray.from("aString", 123, true));

        assertEquals(expected.toString(), query.toN1QL());
    }

    @Test
    public void parametrizedQueryWithObjectShouldProduceStatementAndNamedParameters() {
        JsonObject args = JsonObject.create()
            .put("myParamString", "aString")
            .put("someInt", 123)
            .put("$fullN1qlParam", true);
        ParametrizedQuery query = new ParametrizedQuery(select("*"), args);

        JsonObject expected = JsonObject.create()
            .put("statement", "SELECT *")
            .put("$myParamString", "aString")
            .put("$someInt", 123)
            .put("$fullN1qlParam", true);

        assertEquals(expected.toString(), query.toN1QL());
    }

    @Test
    public void preparedQueryWithObjectShouldProducePreparedAndNamedParameters() {
        JsonObject rawPlan = JsonObject.create().put("fake", "select *");
        QueryPlan fakePlan = new QueryPlan(rawPlan);
        JsonObject args = JsonObject.create()
            .put("myParamString", "aString")
            .put("someInt", 123)
            .put("$fullN1qlParam", true);
        PreparedQuery query = new PreparedQuery(fakePlan, args);

        JsonObject expected = JsonObject.create()
            .put("prepared", rawPlan)
            .put("$myParamString", "aString")
            .put("$someInt", 123)
            .put("$fullN1qlParam", true);

        assertEquals(expected.toString(), query.toN1QL());
    }

}
