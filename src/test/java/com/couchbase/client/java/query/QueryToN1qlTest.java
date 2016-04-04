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

import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.CouchbaseAsyncBucket;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import org.junit.Test;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.couchbase.client.java.query.Select.select;
import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies various combinations of N1QL queries and their transformation to N1QL string.
 *
 * @author Simon Baslé
 * @since 2.1
 */
public class QueryToN1qlTest {

    @Test
    public void simpleQueryShouldJustProduceStatement() {
        SimpleN1qlQuery query = new SimpleN1qlQuery(select("*").from("tutorial").where(x("fname").eq(s("ian"))), null);

        //notice ian is between escaped quotes since inside json
        assertEquals("{\"statement\":\"SELECT * FROM tutorial WHERE fname = \\\"ian\\\"\"}", query.n1ql().toString());
    }

    @Test
    public void rawSimpleQueryShouldJustProduceStatementAsIs() {
        SimpleN1qlQuery query = N1qlQuery.simple("Here goes anything even not \"JSON\"");

        //notice JSON is between escaped quotes since inside json
        assertEquals("{\"statement\":\"Here goes anything even not \\\"JSON\\\"\"}", query.n1ql().toString());
    }

    @Test
    public void parameterizedQueryWithArrayShouldProduceStatementAndArgs() {
        ParameterizedN1qlQuery query = new ParameterizedN1qlQuery(select("*"), JsonArray.from("aString", 123, true), null);

        JsonObject expected = JsonObject.create()
            .put("statement", "SELECT *")
            .put("args", JsonArray.from("aString", 123, true));

        assertEquals(expected, query.n1ql());
    }

    @Test
    public void shouldReuseNameWhenRepreparingPayload() {
        PreparedPayload fakePayload = new PreparedPayload(select("*"), "testName", "plan1234");
        PrepareStatement preparedStatement = PrepareStatement.prepare(fakePayload);

        assertEquals(fakePayload.originalStatement().toString(), preparedStatement.originalStatement().toString());
        assertEquals(fakePayload.preparedName(), preparedStatement.preparedName());
    }

    @Test
    public void shouldNotWrapAPrepareStatement() {
        PrepareStatement statement = PrepareStatement.prepare(select("*"));
        assertEquals(statement, PrepareStatement.prepare(statement));
    }

    @Test
    public void shouldNotDoublePrefixAStringPreparedStatementWithoutName() {
        String alreadyPrepare = "PREPARE SELECT *";
        String secondPrepare = PrepareStatement.prepare(alreadyPrepare).toString();

        assertFalse(secondPrepare.contains("FROM PREPARE"));
        assertFalse(secondPrepare.substring("PREPARE ".length()).contains("PREPARE"));
    }

    @Test
    public void shouldPrependStatementWithPrepare() {
        Statement toPrepare = select("*").from("default");
        PrepareStatement prepare = PrepareStatement.prepare(toPrepare);
        PrepareStatement prepareFromString = PrepareStatement.prepare("SELECT * FROM default");

        Pattern p = Pattern.compile("PREPARE `\\w+` FROM ");
        Matcher prepareMatcher = p.matcher(prepare.toString());
        Matcher prepareFromStringMatcher = p.matcher(prepareFromString.toString());

        assertTrue(prepareMatcher.find());
        assertTrue(prepareFromStringMatcher.find());
        assertEquals("SELECT * FROM default", prepareMatcher.replaceAll(""));
        assertEquals("SELECT * FROM default", prepareFromStringMatcher.replaceAll(""));
    }

    @Test
    public void preparedQueryWithArrayShouldProducePreparedAndArgs() {
        PreparedPayload fakePlan = new PreparedPayload(select("*"), "planName", "plan1234");
        JsonArray params =JsonArray.from("aString", 123, true);
        PreparedN1qlQuery query = new PreparedN1qlQuery(fakePlan, params, null);

        JsonObject expected = JsonObject.create()
            .put("prepared", "planName")
            .put("args", JsonArray.from("aString", 123, true))
            .put("encoded_plan", "plan1234");

        assertEquals(expected, query.n1ql());
    }

    @Test
    public void parameterizedQueryWithObjectShouldProduceStatementAndNamedParameters() {
        JsonObject args = JsonObject.create()
            .put("myParamString", "aString")
            .put("someInt", 123)
            .put("$fullN1qlParam", true);
        ParameterizedN1qlQuery query = new ParameterizedN1qlQuery(select("*"), args, null);

        JsonObject expected = JsonObject.create()
            .put("statement", "SELECT *")
            .put("$myParamString", "aString")
            .put("$someInt", 123)
            .put("$fullN1qlParam", true);

        assertEquals(expected, query.n1ql());
    }

    @Test
    public void preparedQueryWithObjectShouldProducePreparedAndNamedParameters() {
        PreparedPayload fakePlan = new PreparedPayload(select("*"), "planName", "plan1234");
        JsonObject args = JsonObject.create()
            .put("myParamString", "aString")
            .put("someInt", 123)
            .put("$fullN1qlParam", true);
        PreparedN1qlQuery query = new PreparedN1qlQuery(fakePlan, args, null);

        JsonObject expected = JsonObject.create()
            .put("prepared", "planName")
            .put("$myParamString", "aString")
            .put("$someInt", 123)
            .put("$fullN1qlParam", true)
            .put("encoded_plan", "plan1234");

        assertEquals(expected, query.n1ql());
    }

    @Test
    public void queryParamsShouldBeInjectedInQuery() {
        N1qlParams fullParams = N1qlParams.build()
                .consistency(ScanConsistency.REQUEST_PLUS)
                .scanWait(12, TimeUnit.SECONDS)
                .serverSideTimeout(20, TimeUnit.SECONDS)
                .withContextId("test");

        JsonObject expected = JsonObject.create()
                .put("statement", "SELECT * FROM default")
                .put("scan_consistency", "request_plus")
                .put("scan_wait", "12s")
                .put("timeout", "20s")
                .put("client_context_id", "test");

        SimpleN1qlQuery query1 = new SimpleN1qlQuery(select(x("*")).from("default"), fullParams);
        assertEquals(expected, query1.n1ql());

        ParameterizedN1qlQuery query2 = new ParameterizedN1qlQuery(select(x("*")).from("default"), JsonObject.empty(), fullParams);
        assertEquals(expected, query2.n1ql());

        expected
            .removeKey("statement")
            .put("prepared", "planName")
            .put("encoded_plan", "plan1234");
        PreparedN1qlQuery query3 = new PreparedN1qlQuery(new PreparedPayload(select("*"), "planName", "plan1234"), JsonArray.empty(), fullParams);
        assertEquals(expected, query3.n1ql());
    }

    private Object unmarshallSignature(String value) throws Exception {
        return CouchbaseAsyncBucket.JSON_OBJECT_TRANSCODER.byteBufJsonValueToObject(
                Unpooled.copiedBuffer(value, CharsetUtil.UTF_8));
    }

    @Test
    public void testQuerySignaturesAreCorrectlyUnmarshalled() throws Exception {
        Object stringScalar = unmarshallSignature(" \t\n\r\"a\"");
        Object numberScalar = unmarshallSignature(" \t\n\r\n123");
        Object booleanScalar = unmarshallSignature(" \t\n\r\ntrue");
        Object nullScalar = unmarshallSignature(" \t\n\r\nnull");
        Object jsonObject = unmarshallSignature(" \t\n\r\n{\"a\": 123}");
        Object jsonArray = unmarshallSignature(" \t\n\r\n[1, 2, 3]");

        assertTrue(stringScalar.getClass().getSimpleName(), stringScalar instanceof String);
        assertTrue(numberScalar.getClass().getSimpleName(), numberScalar instanceof Number);
        assertTrue(booleanScalar.getClass().getSimpleName(), booleanScalar instanceof Boolean);
        assertNull(nullScalar);
        assertTrue(jsonObject.getClass().getSimpleName(), jsonObject instanceof JsonObject);
        assertTrue(jsonArray.getClass().getSimpleName(), jsonArray instanceof JsonArray);

        assertEquals("a", stringScalar);
        assertEquals(123, numberScalar);
        assertEquals(true, booleanScalar);
        assertNull(nullScalar);
        assertEquals(JsonObject.create().put("a", 123), jsonObject);
        assertEquals(JsonArray.from(1, 2, 3), jsonArray);
    }



}
