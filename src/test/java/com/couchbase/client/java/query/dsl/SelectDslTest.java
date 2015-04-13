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
import com.couchbase.client.java.query.dsl.path.*;
import org.junit.Test;

import static com.couchbase.client.java.query.dsl.Alias.alias;
import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;
import static org.junit.Assert.assertEquals;

/**
 * General tests of the query DSL.
 *
 * @author Michael Nitschinger
 */
public class SelectDslTest {

    //
    // ====================================
    // General Select-From Tests (select-from-clause)
    // ====================================
    //

    @Test
    public void testGroupBy() {
        Path path = new DefaultGroupByPath(null).groupBy(x("relation"));
        assertEquals("GROUP BY relation", path.toString());
    }

    @Test
    public void testGroupByWithHaving() {
        Path path = new DefaultGroupByPath(null).groupBy(x("relation")).having(x("count(*) > 1"));
        assertEquals("GROUP BY relation HAVING count(*) > 1", path.toString());
    }

    @Test
    public void testGroupByWithLetting() {
        Path path = new DefaultGroupByPath(null).groupBy(x("relation")).letting(alias("foo", x("bar")));
        assertEquals("GROUP BY relation LETTING foo = bar", path.toString());
    }

    @Test
    public void testGroupByWithLettingAndHaving() {
        Path path = new DefaultGroupByPath(null)
            .groupBy(x("relation"))
            .letting(alias("foo", x("bar")), alias("hello", s("world")))
            .having(x("count(*) > 1"));
        assertEquals("GROUP BY relation LETTING foo = bar, hello = \"world\" HAVING count(*) > 1", path.toString());
    }

    @Test
    public void testWhere() {
        Path path = new DefaultWherePath(null).where(x("age").gt(x("20")));
        assertEquals("WHERE age > 20", path.toString());

        path = new DefaultWherePath(null).where("age > 20");
        assertEquals("WHERE age > 20", path.toString());
    }

    @Test
    public void testWhereWithGroupBy() {
        Path path = new DefaultWherePath(null).where(x("age > 20")).groupBy(x("age"));
        assertEquals("WHERE age > 20 GROUP BY age", path.toString());
    }

    @Test
    public void testWhereWithGroupByAndHaving() {
        Path path = new DefaultWherePath(null).where(x("age > 20")).groupBy(x("age")).having(x("count(*) > 10"));
        assertEquals("WHERE age > 20 GROUP BY age HAVING count(*) > 10", path.toString());
    }

    @Test
    public void testLet() {
        Path path = new DefaultLetPath(null).let(alias("count", x("COUNT(*)")));
        assertEquals("LET count = COUNT(*)", path.toString());

        path = new DefaultLetPath(null).let(alias("a", x("x > 5")), alias("b", s("foobar")));
        assertEquals("LET a = x > 5, b = \"foobar\"", path.toString());
    }

    @Test
    public void testLetWithWhere() {
        Path path = new DefaultLetPath(null)
            .let(alias("a", x("x > 5")), alias("b", s("foobar")))
            .where(x("foo").eq(s("bar")));
        assertEquals("LET a = x > 5, b = \"foobar\" WHERE foo = \"bar\"", path.toString());
    }

    //
    // ====================================
    // General Select Tests (select-clause)
    // ====================================
    //

    @Test
    public void testSelect() {
        Statement statement = new DefaultSelectPath(null).select(x("firstname"), x("lastname"));
        assertEquals("SELECT firstname, lastname", statement.toString());

        statement = new DefaultSelectPath(null).selectAll(x("firstname"));
        assertEquals("SELECT ALL firstname", statement.toString());
    }

    @Test
    public void testSelectWithUnion() {
        Statement statement = new DefaultSelectPath(null)
            .select(x("firstname"), x("lastname"))
            .union()
            .select(x("a"), x("b"));
        assertEquals("SELECT firstname, lastname UNION SELECT a, b", statement.toString());
    }

    @Test
    public void testOrderBy() {
        Statement statement = new DefaultOrderByPath(null).orderBy(Sort.asc("firstname"));
        assertEquals("ORDER BY firstname ASC", statement.toString());

        statement = new DefaultOrderByPath(null).orderBy(Sort.asc("firstname"), Sort.desc("lastname"));
        assertEquals("ORDER BY firstname ASC, lastname DESC", statement.toString());
    }

    @Test
    public void testOrderByWithLimit() {
        Statement statement = new DefaultOrderByPath(null).orderBy(Sort.asc("firstname")).limit(5);
        assertEquals("ORDER BY firstname ASC LIMIT 5", statement.toString());
    }

    @Test
    public void testOrderByWithLimitAndOffset() {
        Statement statement = new DefaultOrderByPath(null)
            .orderBy(Sort.asc("firstname"), Sort.desc("lastname"))
            .limit(5)
            .offset(10);
        assertEquals("ORDER BY firstname ASC, lastname DESC LIMIT 5 OFFSET 10", statement.toString());
    }

    @Test
    public void testOrderByWithOffset() {
        Statement statement = new DefaultOrderByPath(null)
            .orderBy(Sort.asc("firstname"), Sort.desc("lastname"))
            .offset(3);
        assertEquals("ORDER BY firstname ASC, lastname DESC OFFSET 3", statement.toString());
    }

    @Test
    public void testOffset() {
        Statement statement = new DefaultOffsetPath(null).offset(3);
        assertEquals("OFFSET 3", statement.toString());
    }

    @Test
    public void testLimitWithOffset() {
        Statement statement = new DefaultLimitPath(null).limit(4).offset(3);
        assertEquals("LIMIT 4 OFFSET 3", statement.toString());
    }

    //
    // ====================================
    // From Tests (from-clause)
    // ====================================
    //

    @Test
    public void testSimpleFrom() {
        Statement statement = new DefaultFromPath(null).from("default");
        assertEquals("FROM default", statement.toString());

        statement = new DefaultFromPath(null).from("beer-sample").as("b");
        assertEquals("FROM beer-sample AS b", statement.toString());
    }

    @Test
    public void testFromWithKeys() {
        Statement statement = new DefaultFromPath(null)
            .from("beer-sample")
            .as("b")
            .useKeys("my-brewery");
        assertEquals("FROM beer-sample AS b USE KEYS \"my-brewery\"", statement.toString());

        statement = new DefaultFromPath(null)
            .from("beer-sample")
            .useKeys(JsonArray.from("key1", "key2"));
        assertEquals("FROM beer-sample USE KEYS [\"key1\",\"key2\"]", statement.toString());

        statement = new DefaultFromPath(null)
            .from("beer-sample")
            .useKeys("key1", "key2");
        assertEquals("FROM beer-sample USE KEYS [\"key1\",\"key2\"]", statement.toString());
    }

    @Test
    public void testUnnest() {
        Statement statement = new DefaultFromPath(null)
            .from("tutorial").as("contact")
            .unnest("contact.children")
            .where(x("contact.fname").eq(s("Dave")));
        assertEquals("FROM tutorial AS contact UNNEST contact.children WHERE contact.fname = \"Dave\"",
            statement.toString());

        statement = new DefaultFromPath(null)
            .from("default")
            .leftOuterUnnest("foo.bar")
            .leftUnnest("bar.baz")
            .innerUnnest("x.y");
        assertEquals("FROM default LEFT OUTER UNNEST foo.bar LEFT UNNEST bar.baz INNER UNNEST x.y", statement.toString());
    }

    @Test
    public void testNest() {
        Statement statement = new DefaultFromPath(null)
            .from("users_with_orders").as("user")
            .nest("orders_with_users").as("orders");
        assertEquals("FROM users_with_orders AS user NEST orders_with_users AS orders", statement.toString());

        statement = new DefaultFromPath(null)
            .from("default")
            .leftOuterNest("foo.bar")
            .leftNest("bar.baz")
            .innerNest("x.y");
        assertEquals("FROM default LEFT OUTER NEST foo.bar LEFT NEST bar.baz INNER NEST x.y", statement.toString());
    }

    @Test
    public void testNestWithKeys() {
        Statement statement = new DefaultFromPath(null)
            .from("users_with_orders").as("user")
            .nest("orders_with_users").as("orders")
            .onKeys(x(JsonArray.from("key1", "key2")));
        assertEquals("FROM users_with_orders AS user NEST orders_with_users AS orders ON KEYS [\"key1\",\"key2\"]",
            statement.toString());
    }


    @Test
    public void testJoin() {
        Statement statement = new DefaultFromPath(null)
            .from("users_with_orders").as("user")
            .join("orders_with_users").as("orders");
        assertEquals("FROM users_with_orders AS user JOIN orders_with_users AS orders", statement.toString());

        statement = new DefaultFromPath(null)
            .from("default")
            .leftOuterJoin("foo.bar")
            .leftJoin("bar.baz")
            .innerJoin("x.y");
        assertEquals("FROM default LEFT OUTER JOIN foo.bar LEFT JOIN bar.baz INNER JOIN x.y", statement.toString());
    }

    @Test
    public void testJoinWithKeys() {
        Statement statement = new DefaultFromPath(null)
            .from("users_with_orders").as("user")
            .join("orders_with_users").as("orders")
            .onKeys(x(JsonArray.from("key1", "key2")));
        assertEquals("FROM users_with_orders AS user JOIN orders_with_users AS orders ON KEYS [\"key1\",\"key2\"]",
            statement.toString());
    }

}
