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
import com.couchbase.client.java.query.Statement;
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
        Statement statement = select(s("Hello World")
            .as("Greeting"));
        assertEquals("SELECT \"Hello World\" AS Greeting", statement.toString());
    }

    @Test
    public void test2() {
        Statement statement = select("*")
            .from("tutorial")
            .where(x("fname").eq(s("Ian")));
        assertEquals("SELECT * FROM tutorial WHERE fname = \"Ian\"", statement.toString());
    }

    @Test
    public void test3() {
        Statement statement = select(x("children[0].fname").as("cname"))
            .from("tutorial")
            .where(x("fname").eq(s("Dave")));
        assertEquals("SELECT children[0].fname AS cname FROM tutorial WHERE fname = \"Dave\"", statement.toString());
    }

    @Test
    public void test4() {
        Statement statement = select(meta().as("meta"))
            .from("tutorial");
        assertEquals("SELECT META() AS meta FROM tutorial", statement.toString());
    }

    @Test
    public void test5() {
        Statement statement = select(x("fname"), x("age"), x("age/7").as("age_dog_years"))
            .from("tutorial")
            .where(x("fname").eq(s("Dave")));
        assertEquals("SELECT fname, age, age/7 AS age_dog_years FROM tutorial WHERE fname = \"Dave\"",
            statement.toString());
    }

    @Test
    public void test6() {
        Statement statement = select(x("fname"), x("age"), round(x("age/7")).as("age_dog_years"))
            .from("tutorial")
            .where(x("fname").eq(s("Dave")));
        assertEquals("SELECT fname, age, ROUND(age/7) AS age_dog_years FROM tutorial WHERE fname = \"Dave\"",
            statement.toString());
    }

    @Test
    public void test7() {
        Statement statement = select(x("fname").concat(s(" ")).concat(x("lname")).as("full_name"))
            .from("tutorial");
        assertEquals("SELECT fname || \" \" || lname AS full_name FROM tutorial", statement.toString());
    }

    @Test
    public void test8() {
        Statement statement = select("fname", "age")
            .from("tutorial")
            .where(x("age").gt(x("30")));
        assertEquals("SELECT fname, age FROM tutorial WHERE age > 30", statement.toString());
    }

    @Test
    public void test9() {
        Statement statement = select("fname", "email")
            .from("tutorial")
            .where(x("email").like(s("%@yahoo.com")));
        assertEquals("SELECT fname, email FROM tutorial WHERE email LIKE \"%@yahoo.com\"", statement.toString());
    }

    @Test
    public void test10() {
        Statement statement = selectDistinct("orderlines[0].productId")
            .from("orders");
        assertEquals("SELECT DISTINCT orderlines[0].productId FROM orders", statement.toString());
    }

    @Test
    public void test11() {
        Statement statement = select("fname", "children")
            .from("tutorial")
            .where(x("children").isNull());
        assertEquals("SELECT fname, children FROM tutorial WHERE children IS NULL", statement.toString());
    }

    @Test
    public void test12() {
        Statement statement = select("fname", "children")
            .from("tutorial")
            .where(x("ANY child IN tutorial.children SATISFIES child.age > 10 END"));
        assertEquals("SELECT fname, children FROM tutorial WHERE ANY child IN tutorial.children " +
            "SATISFIES child.age > 10 END", statement.toString());
    }

    @Test
    public void test13() {
        Statement statement = select("fname", "email", "children")
            .from("tutorial")
            .where(length(x("children")).gt(x("0")).and(x("email")).like(s("%@gmail.com")));
        assertEquals("SELECT fname, email, children FROM tutorial WHERE LENGTH(children) > 0 AND email" +
            " LIKE \"%@gmail.com\"", statement.toString());
    }

    @Test
    public void test14() {
        Statement statement = select("fname", "email")
            .from("tutorial")
            .keys(x(JsonArray.from("dave", "ian")));
        assertEquals("SELECT fname, email FROM tutorial KEYS [\"dave\",\"ian\"]", statement.toString());
    }

    @Test
    public void test15() {
        Statement statement = select("children[0:2]")
            .from("tutorial")
            .where(x("children[0:2] IS NOT MISSING"));
        assertEquals("SELECT children[0:2] FROM tutorial WHERE children[0:2] IS NOT MISSING", statement.toString());
    }

}
