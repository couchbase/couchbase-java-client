/*
 * Copyright (c) 2016 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.client.java.query.dsl;

import static com.couchbase.client.java.query.Select.select;
import static com.couchbase.client.java.query.Select.selectDistinct;
import static com.couchbase.client.java.query.dsl.Alias.alias;
import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.path;
import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;
import static org.junit.Assert.assertEquals;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.dsl.path.DefaultFromPath;
import com.couchbase.client.java.query.dsl.path.DefaultGroupByPath;
import com.couchbase.client.java.query.dsl.path.DefaultHintPath;
import com.couchbase.client.java.query.dsl.path.DefaultLetPath;
import com.couchbase.client.java.query.dsl.path.DefaultLimitPath;
import com.couchbase.client.java.query.dsl.path.DefaultOffsetPath;
import com.couchbase.client.java.query.dsl.path.DefaultOrderByPath;
import com.couchbase.client.java.query.dsl.path.DefaultSelectPath;
import com.couchbase.client.java.query.dsl.path.DefaultWherePath;
import com.couchbase.client.java.query.dsl.path.Path;
import com.couchbase.client.java.query.dsl.path.SelectResultPath;
import com.couchbase.client.java.query.dsl.path.index.IndexReference;
import com.couchbase.client.java.query.dsl.path.index.IndexType;
import org.junit.Test;

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

    @Test
    public void testJoins() {
        Expression eToken = Expression.x("a");
        String sToken = "a";

        String pathString = new DefaultLetPath(null).join(sToken).toString();
        String pathExpression = new DefaultLetPath(null).join(eToken).toString();
        assertEquals("JOIN a", pathString);
        assertEquals(pathString, pathExpression);

        pathString = new DefaultLetPath(null).leftJoin(sToken).toString();
        pathExpression = new DefaultLetPath(null).leftJoin(eToken).toString();
        assertEquals("LEFT JOIN a", pathString);
        assertEquals(pathString, pathExpression);

        pathString = new DefaultLetPath(null).innerJoin(sToken).toString();
        pathExpression = new DefaultLetPath(null).innerJoin(eToken).toString();
        assertEquals("INNER JOIN a", pathString);
        assertEquals(pathString, pathExpression);

        pathString = new DefaultLetPath(null).leftOuterJoin(sToken).toString();
        pathExpression = new DefaultLetPath(null).leftOuterJoin(eToken).toString();
        assertEquals("LEFT OUTER JOIN a", pathString);
        assertEquals(pathString, pathExpression);
    }

    @Test
    public void testNests() {
        Expression eToken = Expression.x("a");
        String sToken = "a";

        String pathString = new DefaultLetPath(null).nest(sToken).toString();
        String pathExpression = new DefaultLetPath(null).nest(eToken).toString();
        assertEquals("NEST a", pathString);
        assertEquals(pathString, pathExpression);

        pathString = new DefaultLetPath(null).leftNest(sToken).toString();
        pathExpression = new DefaultLetPath(null).leftNest(eToken).toString();
        assertEquals("LEFT NEST a", pathString);
        assertEquals(pathString, pathExpression);

        pathString = new DefaultLetPath(null).innerNest(sToken).toString();
        pathExpression = new DefaultLetPath(null).innerNest(eToken).toString();
        assertEquals("INNER NEST a", pathString);
        assertEquals(pathString, pathExpression);

        pathString = new DefaultLetPath(null).leftOuterNest(sToken).toString();
        pathExpression = new DefaultLetPath(null).leftOuterNest(eToken).toString();
        assertEquals("LEFT OUTER NEST a", pathString);
        assertEquals(pathString, pathExpression);
    }

    @Test
    public void testUnnests() {
        Expression eToken = Expression.x("a");
        String sToken = "a";

        String pathString = new DefaultLetPath(null).unnest(sToken).toString();
        String pathExpression = new DefaultLetPath(null).unnest(eToken).toString();
        assertEquals("UNNEST a", pathString);
        assertEquals(pathString, pathExpression);

        pathString = new DefaultLetPath(null).leftUnnest(sToken).toString();
        pathExpression = new DefaultLetPath(null).leftUnnest(eToken).toString();
        assertEquals("LEFT UNNEST a", pathString);
        assertEquals(pathString, pathExpression);

        pathString = new DefaultLetPath(null).innerUnnest(sToken).toString();
        pathExpression = new DefaultLetPath(null).innerUnnest(eToken).toString();
        assertEquals("INNER UNNEST a", pathString);
        assertEquals(pathString, pathExpression);

        pathString = new DefaultLetPath(null).leftOuterUnnest(sToken).toString();
        pathExpression = new DefaultLetPath(null).leftOuterUnnest(eToken).toString();
        assertEquals("LEFT OUTER UNNEST a", pathString);
        assertEquals(pathString, pathExpression);
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
    public void testSelectWithUnionAll() {
        Statement statement = new DefaultSelectPath(null)
            .select(x("firstname"), x("lastname"))
            .unionAll()
            .select(x("a"), x("b"));

        assertEquals("SELECT firstname, lastname UNION ALL SELECT a, b", statement.toString());
    }

    @Test
    public void testSelectWithIntersect() {
        Statement statement = new DefaultSelectPath(null)
            .select(x("firstname"), x("lastname"))
            .from("foo")
            .where(x("lastname").eq(s("foo")))
            .intersect()
            .select(x("firstname"), x("lastname"))
            .from("foo")
            .where(x("lastname").eq(s("bar")));

        String expected = "SELECT firstname, lastname "
            + "FROM foo "
            + "WHERE lastname = \"foo\" "
            + "INTERSECT "
            + "SELECT firstname, lastname "
            + "FROM foo "
            + "WHERE lastname = \"bar\"";

        assertEquals(expected, statement.toString());
    }

    @Test
    public void testSelectWithIntersectAll() {
        Statement statement = new DefaultSelectPath(null)
            .select(x("firstname"), x("lastname"))
            .from("foo")
            .where(x("lastname").eq(s("foo")))
            .intersectAll()
            .select(x("firstname"), x("lastname"))
            .from("foo")
            .where(x("lastname").eq(s("bar")));

        String expected = "SELECT firstname, lastname "
            + "FROM foo "
            + "WHERE lastname = \"foo\" "
            + "INTERSECT ALL "
            + "SELECT firstname, lastname "
            + "FROM foo "
            + "WHERE lastname = \"bar\"";

        assertEquals(expected, statement.toString());
    }

    @Test
    public void testSelectWithExcept() {
        Statement statement = new DefaultSelectPath(null)
            .select(x("firstname"), x("lastname"))
            .from("foo")
            .where(x("lastname").eq(s("foo")))
            .except()
            .select(x("firstname"), x("lastname"))
            .from("foo")
            .where(x("lastname").eq(s("bar")));

        String expected = "SELECT firstname, lastname "
            + "FROM foo "
            + "WHERE lastname = \"foo\" "
            + "EXCEPT "
            + "SELECT firstname, lastname "
            + "FROM foo "
            + "WHERE lastname = \"bar\"";

        assertEquals(expected, statement.toString());
    }

    @Test
    public void testSelectWithExceptAll() {
        Statement statement = new DefaultSelectPath(null)
            .select(x("firstname"), x("lastname"))
            .from("foo")
            .where(x("lastname").eq(s("foo")))
            .exceptAll()
            .select(x("firstname"), x("lastname"))
            .from("foo")
            .where(x("lastname").eq(s("bar")));

        String expected = "SELECT firstname, lastname "
            + "FROM foo "
            + "WHERE lastname = \"foo\" "
            + "EXCEPT ALL "
            + "SELECT firstname, lastname "
            + "FROM foo "
            + "WHERE lastname = \"bar\"";

        assertEquals(expected, statement.toString());
    }

    @Test
    public void testSelectChainedWithUnion() {
        SelectResultPath statement1 = new DefaultSelectPath(null)
            .select(x("firstname"), x("lastname"))
            .from("foo")
            .where(x("lastname").eq(s("foo")));

        SelectResultPath statement2 = new DefaultSelectPath(null)
            .select(x("firstname"), x("lastname"))
            .from("foo")
            .where(x("lastname").eq(s("bar")));

        String expected = "SELECT firstname, lastname "
            + "FROM foo "
            + "WHERE lastname = \"foo\" "
            + "UNION "
            + "SELECT firstname, lastname "
            + "FROM foo "
            + "WHERE lastname = \"bar\"";

        assertEquals(expected, statement1.union(statement2).toString());
    }

    @Test
    public void testSelectChainedWithUnionAll() {
        SelectResultPath statement1 = new DefaultSelectPath(null)
            .select(x("firstname"), x("lastname"))
            .from("foo")
            .where(x("lastname").eq(s("foo")));

        SelectResultPath statement2 = new DefaultSelectPath(null)
            .select(x("firstname"), x("lastname"))
            .from("foo")
            .where(x("lastname").eq(s("bar")));

        String expected = "SELECT firstname, lastname "
            + "FROM foo "
            + "WHERE lastname = \"foo\" "
            + "UNION ALL "
            + "SELECT firstname, lastname "
            + "FROM foo "
            + "WHERE lastname = \"bar\"";

        assertEquals(expected, statement1.unionAll(statement2).toString());
    }

    @Test
    public void testSelectChainedWithIntersect() {
        SelectResultPath statement1 = new DefaultSelectPath(null)
            .select(x("firstname"), x("lastname"))
            .from("foo")
            .where(x("lastname").eq(s("foo")));

        SelectResultPath statement2 = new DefaultSelectPath(null)
            .select(x("firstname"), x("lastname"))
            .from("foo")
            .where(x("lastname").eq(s("bar")));

        String expected = "SELECT firstname, lastname "
            + "FROM foo "
            + "WHERE lastname = \"foo\" "
            + "INTERSECT "
            + "SELECT firstname, lastname "
            + "FROM foo "
            + "WHERE lastname = \"bar\"";

        assertEquals(expected, statement1.intersect(statement2).toString());
    }

    @Test
    public void testSelectChainedWithIntersectAll() {
        SelectResultPath statement1 = new DefaultSelectPath(null)
            .select(x("firstname"), x("lastname"))
            .from("foo")
            .where(x("lastname").eq(s("foo")));

        SelectResultPath statement2 = new DefaultSelectPath(null)
            .select(x("firstname"), x("lastname"))
            .from("foo")
            .where(x("lastname").eq(s("bar")));

        String expected = "SELECT firstname, lastname "
            + "FROM foo "
            + "WHERE lastname = \"foo\" "
            + "INTERSECT ALL "
            + "SELECT firstname, lastname "
            + "FROM foo "
            + "WHERE lastname = \"bar\"";

        assertEquals(expected, statement1.intersectAll(statement2).toString());
    }

    @Test
    public void testSelectChainedWithExcept() {
        SelectResultPath statement1 = new DefaultSelectPath(null)
            .select(x("firstname"), x("lastname"))
            .from("foo")
            .where(x("lastname").eq(s("foo")));

        SelectResultPath statement2 = new DefaultSelectPath(null)
            .select(x("firstname"), x("lastname"))
            .from("foo")
            .where(x("lastname").eq(s("bar")));

        String expected = "SELECT firstname, lastname "
            + "FROM foo "
            + "WHERE lastname = \"foo\" "
            + "EXCEPT "
            + "SELECT firstname, lastname "
            + "FROM foo "
            + "WHERE lastname = \"bar\"";

        assertEquals(expected, statement1.except(statement2).toString());
    }

    @Test
    public void testSelectChainedWithExceptAll() {
        SelectResultPath statement1 = new DefaultSelectPath(null)
            .select(x("firstname"), x("lastname"))
            .from("foo")
            .where(x("lastname").eq(s("foo")));

        SelectResultPath statement2 = new DefaultSelectPath(null)
            .select(x("firstname"), x("lastname"))
            .from("foo")
            .where(x("lastname").eq(s("bar")));

        String expected = "SELECT firstname, lastname "
            + "FROM foo "
            + "WHERE lastname = \"foo\" "
            + "EXCEPT ALL "
            + "SELECT firstname, lastname "
            + "FROM foo "
            + "WHERE lastname = \"bar\"";

        assertEquals(expected, statement1.exceptAll(statement2).toString());
    }

    @Test
    public void testOrderBy() {
        Statement statement = new DefaultOrderByPath(null).orderBy(Sort.asc("firstname"));
        assertEquals("ORDER BY firstname ASC", statement.toString());

        statement = new DefaultOrderByPath(null).orderBy(Sort.asc("firstname"), Sort.desc("lastname"));
        assertEquals("ORDER BY firstname ASC, lastname DESC", statement.toString());

        statement = new DefaultOrderByPath(null).orderBy(Sort.def("firstname"), Sort.def(x("lastname")));
        assertEquals("ORDER BY firstname, lastname", statement.toString());
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

    @Test
    public void testHintIndexPathSingle() {
        Statement hint1 = new DefaultHintPath(null).useIndex(IndexReference.indexRef("test"));
        Statement hint2 = new DefaultHintPath(null).useIndex("test");


        assertEquals("USE INDEX (`test`)", hint1.toString());
        assertEquals(hint1.toString(), hint2.toString());

        Statement typedHint1 = new DefaultHintPath(null).useIndex(IndexReference.indexRef("test", IndexType.GSI));
        Statement typedHint2 = new DefaultHintPath(null).useIndex(IndexReference.indexRef("test", IndexType.VIEW));

        assertEquals("USE INDEX (`test` USING GSI)", typedHint1.toString());
        assertEquals("USE INDEX (`test` USING VIEW)", typedHint2.toString());
    }

    @Test
    public void testHintIndexPathMultiple() {
        Statement hint1 = new DefaultHintPath(null).useIndex(IndexReference.indexRef("test"), IndexReference.indexRef("test2"));
        Statement hint2 = new DefaultHintPath(null).useIndex("test", "test2");

        assertEquals("USE INDEX (`test`,`test2`)", hint1.toString());
        assertEquals(hint1.toString(), hint2.toString());

        Statement typedHint1 = new DefaultHintPath(null).useIndex(
                IndexReference.indexRef("test", IndexType.GSI),
                IndexReference.indexRef("test", IndexType.VIEW));

        assertEquals("USE INDEX (`test` USING GSI,`test` USING VIEW)", typedHint1.toString());
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
            .useKeys("a.id");
        assertEquals("FROM beer-sample AS b USE KEYS a.id", statement.toString());

        statement = new DefaultFromPath(null)
            .from("beer-sample")
            .as("b")
            .useKeysValues("my-brewery");
        assertEquals("FROM beer-sample AS b USE KEYS \"my-brewery\"", statement.toString());

        statement = new DefaultFromPath(null)
            .from("beer-sample")
            .useKeys(JsonArray.from("key1", "key2"));
        assertEquals("FROM beer-sample USE KEYS [\"key1\",\"key2\"]", statement.toString());

        statement = new DefaultFromPath(null)
            .from("beer-sample")
            .useKeysValues("key1", "key2");
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

        statement = new DefaultFromPath(null)
                .from("users_with_orders").as("user")
                .join("orders_with_users").as("orders")
                .onKeys(JsonArray.from("key1", "key2"));
        assertEquals("FROM users_with_orders AS user JOIN orders_with_users AS orders ON KEYS [\"key1\",\"key2\"]",
                statement.toString());

        statement = new DefaultFromPath(null)
                .from("users_with_orders").as("user")
                .join("orders_with_users").as("orders")
                .onKeys("orders.id");
        assertEquals("FROM users_with_orders AS user JOIN orders_with_users AS orders ON KEYS orders.id",
                statement.toString());
    }

    @Test
    public void testJoinWithEscapedNamespace() {
        Statement statement = new DefaultFromPath(null).from("a")
                                   .join(i("beer-sample")).as("b")
                                   .onKeys(path("a", "foreignKey"));

        assertEquals("FROM a JOIN `beer-sample` AS b ON KEYS a.foreignKey", statement.toString());
    }

    //
    // ====================================
    // ANSI Join Tests
    // ====================================
    //

    @Test
    public void testSimpleAnsiJoin() {
        Expression where = x("airport.type").eq(s("airport"))
                .and("airport.city").eq(s("San Francisco"))
                .and("airport.country").eq(s("United States"));

        Statement statement = selectDistinct("route.destinationairport")
                .from(i("travel-sample") + " airport")
                .join(i("travel-sample") + " route")
                .on(x("airport.faa = route.sourceairport").and("route.type = " + s("route")))
                .where(where);

        String expected = "SELECT DISTINCT route.destinationairport FROM `travel-sample` airport JOIN " +
                "`travel-sample` route ON airport.faa = route.sourceairport AND route.type = \"route\" " +
                "WHERE airport.type = \"airport\" AND airport.city = \"San Francisco\" AND " +
                "airport.country = \"United States\"";
        assertEquals(expected, statement.toString());
    }

    @Test
    public void testAnsiJoinWithOr() {
        Expression onClause = x("hotel.city").eq("landmark.city")
                .and("hotel.country").eq("landmark.country")
                .and("landmark.type").eq(s("landmark"));

        Expression whereClause = x("hotel.type").eq(s("hotel"))
                .and("hotel.title").like(s("Yosemite%"))
                .and("array_length(hotel.public_likes)").gt(5);

        Statement statement = select(
                "hotel.name hotel_name", "landmark.name landmark_name", "landmark.activity"
        )
            .from(i("travel-sample") + " hotel")
            .join(i("travel-sample") + " landmark")
                .on(onClause)
                .where(whereClause);

        String expected = "SELECT hotel.name hotel_name, landmark.name landmark_name, landmark.activity " +
                "FROM `travel-sample` hotel JOIN `travel-sample` landmark ON hotel.city = landmark.city " +
                "AND hotel.country = landmark.country AND landmark.type = \"landmark\" " +
                "WHERE hotel.type = \"hotel\" AND hotel.title LIKE \"Yosemite%\" AND " +
                "array_length(hotel.public_likes) > 5";

        assertEquals(expected, statement.toString());

    }

}
