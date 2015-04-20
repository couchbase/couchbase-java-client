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

import static com.couchbase.client.java.query.Select.select;
import static com.couchbase.client.java.query.Select.selectDistinct;
import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;
import static com.couchbase.client.java.query.dsl.Functions.round;
import static com.couchbase.client.java.query.dsl.functions.AggregateFunctions.avg;
import static com.couchbase.client.java.query.dsl.functions.AggregateFunctions.count;
import static com.couchbase.client.java.query.dsl.functions.ArrayFunctions.arrayLength;
import static com.couchbase.client.java.query.dsl.functions.MetaFunctions.meta;
import static org.junit.Assert.assertEquals;

import javax.swing.plaf.nimbus.State;

import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.dsl.functions.AggregateFunctions;
import org.junit.Test;

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
        Statement statement = select(meta("tutorial").as("meta"))
            .from("tutorial");
        assertEquals("SELECT META(tutorial) AS meta FROM tutorial", statement.toString());
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
                //TODO implement ANY?
            .where(x("ANY child IN tutorial.children SATISFIES child.age > 10 END"));
        assertEquals("SELECT fname, children FROM tutorial WHERE ANY child IN tutorial.children " +
            "SATISFIES child.age > 10 END", statement.toString());
    }

    @Test
    public void test13() {
        Statement statement = select("fname", "email", "children")
            .from("tutorial")
            .where(arrayLength("children").gt(x("0")).and(x("email")).like(s("%@gmail.com")));
        assertEquals("SELECT fname, email, children FROM tutorial WHERE ARRAY_LENGTH(children) > 0 AND email" +
            " LIKE \"%@gmail.com\"", statement.toString());
    }

    @Test
    public void test14() {
        Statement statement = select("fname", "email")
            .from("tutorial")
            .useKeys("dave", "ian");
        assertEquals("SELECT fname, email FROM tutorial USE KEYS [\"dave\",\"ian\"]", statement.toString());
    }

    @Test
    public void test15() {
        Statement statement = select("children[0:2]")
            .from("tutorial")
            .where(x("children[0:2]").isNotMissing());
        assertEquals("SELECT children[0:2] FROM tutorial WHERE children[0:2] IS NOT MISSING", statement.toString());
    }

    @Test
    public void test16() {
        Statement statement = select(x("fname").concat("\" \"").concat("lname").as("full_name"),
                x("email"), x("children[0:2]").as("offsprings"))
                .from("tutorial")
                .where(
                        x("email").like(s("%@yahoo.com"))
                                //TODO add ANY function/path
                                .or(x("ANY child IN tutorial.children SATISFIES child.age > 10 END")));
        assertEquals("SELECT fname || \" \" || lname AS full_name, email, children[0:2] AS offsprings " +
                "FROM tutorial WHERE email LIKE \"%@yahoo.com\" " +
                "OR ANY child IN tutorial.children SATISFIES child.age > 10 END",
                statement.toString());
    }

    @Test
    public void test17() {
        Statement statement = select("fname", "age").from("tutorial").orderBy(Sort.def("age"));
        assertEquals("SELECT fname, age FROM tutorial ORDER BY age", statement.toString());
    }

    @Test
    public void test18() {
        Statement statement = select("fname", "age")
                .from("tutorial")
                .orderBy(Sort.def("age")).limit(2);
        assertEquals("SELECT fname, age FROM tutorial ORDER BY age LIMIT 2", statement.toString());
    }

    @Test
    public void test19() {
        Statement statement = select(count("*").as("count"))
                .from("tutorial");
        assertEquals("SELECT COUNT(*) AS count FROM tutorial", statement.toString());
    }

    @Test
    public void test20() {
        Statement statement = select(x("relation"), count("*").as("count"))
                .from("tutorial").groupBy(x("relation"));
        assertEquals("SELECT relation, COUNT(*) AS count FROM tutorial GROUP BY relation", statement.toString());
    }

    @Test
    public void test21() {
        Statement statement = select(x("relation"), count("*").as("count"))
                .from("tutorial").groupBy(x("relation"))
                .having(count("*").gt(1));
        assertEquals("SELECT relation, COUNT(*) AS count FROM tutorial GROUP BY relation " +
                "HAVING COUNT(*) > 1", statement.toString());
    }

    @Test
    public void test22() {
        //TODO add ARRAY comprehension
        Statement statement = select(x("ARRAY child.fname FOR child IN tutorial.children END").as("children_names"))
                .from("tutorial").where(x("children").isNotNull());
        assertEquals("SELECT ARRAY child.fname FOR child IN tutorial.children END AS children_names " +
                "FROM tutorial WHERE children IS NOT NULL", statement.toString());
    }

    @Test
    public void test23() {
        Statement statement = select(x("t.relation"), count("*").as("count"), avg("c.age").as("avg_age"))
                .from("tutorial").as("t")
                .unnest("t.children").as("c")
                .where(x("c.age").gt(10))
                .groupBy(x("t.relation"))
                .having(count("*").gt(1))
                .orderBy(Sort.desc("avg_age"))
                .limit(1).offset(1);

        //NOTE: the AS clause in the tutorial uses the shorthand "tutorial t"
        //we only support the extended syntax "tutorial AS t"
        //(the other one brings no real value in the context of the DSL)
        assertEquals("SELECT t.relation, COUNT(*) AS count, AVG(c.age) AS avg_age " +
                "FROM tutorial AS t " +
                "UNNEST t.children AS c " +
                "WHERE c.age > 10 " +
                "GROUP BY t.relation " +
                "HAVING COUNT(*) > 1 " +
                "ORDER BY avg_age DESC " +
                "LIMIT 1 OFFSET 1", statement.toString());
    }

    @Test
    public void test24() {
        Statement statement = select("usr.personal_details", "orders")
                .from("users_with_orders").as("usr")
                .useKeys("Elinor_33313792")
                .join("orders_with_users").as("orders")
                .onKeys(x("ARRAY s.order_id FOR s IN usr.shipped_order_history END"));
        assertEquals("SELECT usr.personal_details, orders " +
                "FROM users_with_orders AS usr " +
                "USE KEYS \"Elinor_33313792\" " +
                "JOIN orders_with_users AS orders " +
                "ON KEYS ARRAY s.order_id FOR s IN usr.shipped_order_history END",
                statement.toString());
    }

    @Test
    public void test25() {
        Statement statement = select("usr.personal_details", "orders")
                .from("users_with_orders").as("usr")
                .useKeys("Tamekia_13483660")
                .leftJoin("orders_with_users").as("orders")
                //TODO collection ARRAY
                .onKeys(x("ARRAY s.order_id FOR s IN usr.shipped_order_history END"));
        assertEquals("SELECT usr.personal_details, orders " +
                "FROM users_with_orders AS usr " +
                "USE KEYS \"Tamekia_13483660\" " +
                "LEFT JOIN orders_with_users AS orders " +
                "ON KEYS ARRAY s.order_id FOR s IN usr.shipped_order_history END", statement.toString());
    }
}
