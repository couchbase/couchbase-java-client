package com.couchbase.client.java.query.dsl;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.query.Query;
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
        Query query = new DefaultSelectPath(null).select(x("firstname"), x("lastname"));
        assertEquals("SELECT firstname, lastname", query.toString());

        query = new DefaultSelectPath(null).selectAll(x("firstname"));
        assertEquals("SELECT ALL firstname", query.toString());
    }

    @Test
    public void testSelectWithUnion() {
        Query query = new DefaultSelectPath(null)
            .select(x("firstname"), x("lastname"))
            .union()
            .select(x("a"), x("b"));
        assertEquals("SELECT firstname, lastname UNION SELECT a, b", query.toString());
    }

    @Test
    public void testOrderBy() {
        Query query = new DefaultOrderByPath(null).orderBy(Sort.asc("firstname"));
        assertEquals("ORDER BY firstname ASC", query.toString());

        query = new DefaultOrderByPath(null).orderBy(Sort.asc("firstname"), Sort.desc("lastname"));
        assertEquals("ORDER BY firstname ASC, lastname DESC", query.toString());
    }

    @Test
    public void testOrderByWithLimit() {
        Query query = new DefaultOrderByPath(null).orderBy(Sort.asc("firstname")).limit(5);
        assertEquals("ORDER BY firstname ASC LIMIT 5", query.toString());
    }

    @Test
    public void testOrderByWithLimitAndOffset() {
        Query query = new DefaultOrderByPath(null)
            .orderBy(Sort.asc("firstname"), Sort.desc("lastname"))
            .limit(5)
            .offset(10);
        assertEquals("ORDER BY firstname ASC, lastname DESC LIMIT 5 OFFSET 10", query.toString());
    }

    @Test
    public void testOrderByWithOffset() {
        Query query = new DefaultOrderByPath(null)
            .orderBy(Sort.asc("firstname"), Sort.desc("lastname"))
            .offset(3);
        assertEquals("ORDER BY firstname ASC, lastname DESC OFFSET 3", query.toString());
    }

    @Test
    public void testOffset() {
        Query query = new DefaultOffsetPath(null).offset(3);
        assertEquals("OFFSET 3", query.toString());
    }

    @Test
    public void testLimitWithOffset() {
        Query query = new DefaultLimitPath(null).limit(4).offset(3);
        assertEquals("LIMIT 4 OFFSET 3", query.toString());
    }

    //
    // ====================================
    // From Tests (from-clause)
    // ====================================
    //

    @Test
    public void testSimpleFrom() {
        Query query = new DefaultFromPath(null).from("default");
        assertEquals("FROM default", query.toString());

        query = new DefaultFromPath(null).from("beer-sample").as("b");
        assertEquals("FROM beer-sample AS b", query.toString());
    }

    @Test
    public void testFromWithKeys() {
        Query query = new DefaultFromPath(null)
            .from("beer-sample")
            .as("b")
            .keys("my-brewery");
        assertEquals("FROM beer-sample AS b KEYS [\"my-brewery\"]", query.toString());

        query = new DefaultFromPath(null)
            .from("beer-sample")
            .keys(JsonArray.from("key1", "key2"));
        assertEquals("FROM beer-sample KEYS [\"key1\",\"key2\"]", query.toString());
    }

    @Test
    public void testUnnest() {
        Query query = new DefaultFromPath(null)
            .from("tutorial").as("contact")
            .unnest("contact.children")
            .where(x("contact.fname").eq(s("Dave")));
        assertEquals("FROM tutorial AS contact UNNEST contact.children WHERE contact.fname = \"Dave\"",
            query.toString());

        query = new DefaultFromPath(null)
            .from("default")
            .leftOuterUnnest("foo.bar")
            .leftUnnest("bar.baz")
            .innerUnnest("x.y");
        assertEquals("FROM default LEFT OUTER UNNEST foo.bar LEFT UNNEST bar.baz INNER UNNEST x.y", query.toString());
    }

    @Test
    public void testNest() {
        Query query = new DefaultFromPath(null)
            .from("users_with_orders").as("user")
            .nest("orders_with_users").as("orders");
        assertEquals("FROM users_with_orders AS user NEST orders_with_users AS orders", query.toString());

        query = new DefaultFromPath(null)
            .from("default")
            .leftOuterNest("foo.bar")
            .leftNest("bar.baz")
            .innerNest("x.y");
        assertEquals("FROM default LEFT OUTER NEST foo.bar LEFT NEST bar.baz INNER NEST x.y", query.toString());
    }

    @Test
    public void testNestWithKeys() {
        Query query = new DefaultFromPath(null)
            .from("users_with_orders").as("user")
            .nest("orders_with_users").as("orders")
            .keys(x(JsonArray.from("key1", "key2")));
        assertEquals("FROM users_with_orders AS user NEST orders_with_users AS orders KEYS [\"key1\",\"key2\"]",
            query.toString());
    }


    @Test
    public void testJoin() {
        Query query = new DefaultFromPath(null)
            .from("users_with_orders").as("user")
            .join("orders_with_users").as("orders");
        assertEquals("FROM users_with_orders AS user JOIN orders_with_users AS orders", query.toString());

        query = new DefaultFromPath(null)
            .from("default")
            .leftOuterJoin("foo.bar")
            .leftJoin("bar.baz")
            .innerJoin("x.y");
        assertEquals("FROM default LEFT OUTER JOIN foo.bar LEFT JOIN bar.baz INNER JOIN x.y", query.toString());
    }

    @Test
    public void testJoinWithKeys() {
        Query query = new DefaultFromPath(null)
            .from("users_with_orders").as("user")
            .join("orders_with_users").as("orders")
            .keys(x(JsonArray.from("key1", "key2")));
        assertEquals("FROM users_with_orders AS user JOIN orders_with_users AS orders KEYS [\"key1\",\"key2\"]",
            query.toString());
    }

}
