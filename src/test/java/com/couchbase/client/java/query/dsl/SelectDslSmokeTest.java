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
import com.couchbase.client.java.query.Query;
import org.junit.Test;

import static com.couchbase.client.java.query.Select.select;
import static com.couchbase.client.java.query.Select.selectDistinct;
import static com.couchbase.client.java.query.dsl.Expression.*;
import static com.couchbase.client.java.query.dsl.Functions.length;
import static com.couchbase.client.java.query.dsl.Functions.meta;
import static com.couchbase.client.java.query.dsl.Functions.round;
import static org.junit.Assert.assertEquals;

/**
 * These tests resemble the queries from the online tutorial.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class SelectDslSmokeTest {

    @Test
    public void test1() {
        Query query = select(s("Hello World")
            .as("Greeting"));
        assertEquals("SELECT \"Hello World\" AS Greeting", query.toString());
    }

    @Test
    public void test2() {
        Query query = select("*")
            .from("tutorial")
            .where(x("fname").eq(s("Ian")));
        assertEquals("SELECT * FROM tutorial WHERE fname = \"Ian\"", query.toString());
    }

    @Test
    public void test3() {
        Query query = select(x("children[0].fname").as("cname"))
            .from("tutorial")
            .where(x("fname").eq(s("Dave")));
        assertEquals("SELECT children[0].fname AS cname FROM tutorial WHERE fname = \"Dave\"", query.toString());
    }

    @Test
    public void test4() {
        Query query = select(meta().as("meta"))
            .from("tutorial");
        assertEquals("SELECT META() AS meta FROM tutorial", query.toString());
    }

    @Test
    public void test5() {
        Query query = select(x("fname"), x("age"), x("age/7").as("age_dog_years"))
            .from("tutorial")
            .where(x("fname").eq(s("Dave")));
        assertEquals("SELECT fname, age, age/7 AS age_dog_years FROM tutorial WHERE fname = \"Dave\"",
            query.toString());
    }

    @Test
    public void test6() {
        Query query = select(x("fname"), x("age"), round(x("age/7")).as("age_dog_years"))
            .from("tutorial")
            .where(x("fname").eq(s("Dave")));
        assertEquals("SELECT fname, age, ROUND(age/7) AS age_dog_years FROM tutorial WHERE fname = \"Dave\"",
            query.toString());
    }

    @Test
    public void test7() {
        Query query = select(x("fname").concat(s(" ")).concat(x("lname")).as("full_name"))
            .from("tutorial");
        assertEquals("SELECT fname || \" \" || lname AS full_name FROM tutorial", query.toString());
    }

    @Test
    public void test8() {
        Query query = select("fname", "age")
            .from("tutorial")
            .where(x("age").gt(x("30")));
        assertEquals("SELECT fname, age FROM tutorial WHERE age > 30", query.toString());
    }

    @Test
    public void test9() {
        Query query = select("fname", "email")
            .from("tutorial")
            .where(x("email").like(s("%@yahoo.com")));
        assertEquals("SELECT fname, email FROM tutorial WHERE email LIKE \"%@yahoo.com\"", query.toString());
    }

    @Test
    public void test10() {
        Query query = selectDistinct("orderlines[0].productId")
            .from("orders");
        assertEquals("SELECT DISTINCT orderlines[0].productId FROM orders", query.toString());
    }

    @Test
    public void test11() {
        Query query = select("fname", "children")
            .from("tutorial")
            .where(x("children").is(NULL()));
        assertEquals("SELECT fname, children FROM tutorial WHERE children IS NULL", query.toString());
    }

    @Test
    public void test12() {
        Query query = select("fname", "children")
            .from("tutorial")
            .where(x("ANY child IN tutorial.children SATISFIES child.age > 10 END"));
        assertEquals("SELECT fname, children FROM tutorial WHERE ANY child IN tutorial.children " +
            "SATISFIES child.age > 10 END", query.toString());
    }

    @Test
    public void test13() {
        Query query = select("fname", "email", "children")
            .from("tutorial")
            .where(length(x("children")).gt(x("0")).and(x("email")).like(s("%@gmail.com")));
        assertEquals("SELECT fname, email, children FROM tutorial WHERE LENGTH(children) > 0 AND email" +
            " LIKE \"%@gmail.com\"", query.toString());
    }

    @Test
    public void test14() {
        Query query = select("fname", "email")
            .from("tutorial")
            .keys(x(JsonArray.from("dave", "ian")));
        assertEquals("SELECT fname, email FROM tutorial KEYS [\"dave\",\"ian\"]", query.toString());
    }

    @Test
    public void test15() {
        Query query = select("children[0:2]")
            .from("tutorial")
            .where(x("children[0:2] IS NOT MISSING"));
        assertEquals("SELECT children[0:2] FROM tutorial WHERE children[0:2] IS NOT MISSING", query.toString());
    }

}
