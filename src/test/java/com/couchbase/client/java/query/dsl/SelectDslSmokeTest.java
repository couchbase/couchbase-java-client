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

import static com.couchbase.client.java.query.Select.*;
import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.path;
import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.sub;
import static com.couchbase.client.java.query.dsl.Expression.x;
import static com.couchbase.client.java.query.dsl.functions.AggregateFunctions.avg;
import static com.couchbase.client.java.query.dsl.functions.AggregateFunctions.count;
import static com.couchbase.client.java.query.dsl.functions.AggregateFunctions.distinct;
import static com.couchbase.client.java.query.dsl.functions.AggregateFunctions.sum;
import static com.couchbase.client.java.query.dsl.functions.ArrayFunctions.arrayLength;
import static com.couchbase.client.java.query.dsl.functions.Case.caseSearch;
import static com.couchbase.client.java.query.dsl.functions.Collections.anyIn;
import static com.couchbase.client.java.query.dsl.functions.Collections.arrayIn;
import static com.couchbase.client.java.query.dsl.functions.DateFunctions.DatePartExt.month;
import static com.couchbase.client.java.query.dsl.functions.DateFunctions.DatePartExt.year;
import static com.couchbase.client.java.query.dsl.functions.DateFunctions.datePartStr;
import static com.couchbase.client.java.query.dsl.functions.DateFunctions.strToMillis;
import static com.couchbase.client.java.query.dsl.functions.MetaFunctions.meta;
import static com.couchbase.client.java.query.dsl.functions.NumberFunctions.round;
import static com.couchbase.client.java.query.dsl.functions.StringFunctions.lower;
import static com.couchbase.client.java.query.dsl.functions.StringFunctions.substr;
import static org.junit.Assert.assertEquals;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.dsl.functions.DateFunctions;
import com.couchbase.client.java.query.dsl.path.HashSide;
import org.junit.Ignore;
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
            .where(anyIn("child", x("tutorial.children")).satisfies(x("child.age").gt(10)));

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
            .useKeysValues("dave", "ian");

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
                        .or(anyIn("child", x("tutorial.children")).satisfies(x("child.age").gt(10)))
                );

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
        Statement statement = select(arrayIn(x("child.fname"), "child", x("tutorial.children")).end().as("children_names"))
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
                .useKeysValues("Elinor_33313792")
                .join("orders_with_users").as("orders")
                .onKeys(arrayIn(x("s.order_id"), "s", x("usr.shipped_order_history")).end());

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
                .useKeysValues("Tamekia_13483660")
                .leftJoin("orders_with_users").as("orders")
                .onKeys(arrayIn(x("s.order_id"), "s", x("usr.shipped_order_history")).end());

        assertEquals("SELECT usr.personal_details, orders " +
                "FROM users_with_orders AS usr " +
                "USE KEYS \"Tamekia_13483660\" " +
                "LEFT JOIN orders_with_users AS orders " +
                "ON KEYS ARRAY s.order_id FOR s IN usr.shipped_order_history END", statement.toString());
    }

    @Test
    public void test26() {
        Statement statement = select("usr.personal_details", "orders")
                .from("users_with_orders").as("usr")
                .useKeysValues("Elinor_33313792")
                .nest("orders_with_users").as("orders")
                .onKeys(arrayIn(x("s.order_id"), "s", x("usr.shipped_order_history")).end());

        assertEquals("SELECT usr.personal_details, orders " +
                "FROM users_with_orders AS usr " +
                "USE KEYS \"Elinor_33313792\" " +
                "NEST orders_with_users AS orders " +
                "ON KEYS ARRAY s.order_id FOR s IN usr.shipped_order_history END", statement.toString());
    }

    @Test
    public void test27() {
        Statement statement = select("*").from("tutorial").as("contact")
                .unnest("contact.children").where(x("contact.fname").eq(s("Dave")));

        assertEquals("SELECT * FROM tutorial AS contact UNNEST contact.children " +
                "WHERE contact.fname = \"Dave\"", statement.toString());
    }


    @Test
    public void test28() {
        Statement statement = select(x("u.personal_details.display_name").as("name"),
                x("s").as("order_no"), x("o.product_details"))
                .from("users_with_orders").as("u")
                .useKeys(s("Aide_48687583"))
                .unnest("u.shipped_order_history").as("s")
                .join("users_with_orders").as("o").onKeys("s.order_id");

        assertEquals("SELECT u.personal_details.display_name AS name, s AS order_no, o.product_details " +
                "FROM users_with_orders AS u USE KEYS \"Aide_48687583\" " +
                "UNNEST u.shipped_order_history AS s " +
                "JOIN users_with_orders AS o ON KEYS s.order_id", statement.toString());
    }

    @Test
    @Ignore("needs EXPLAIN and INSERT dsl")
    public void test29() {
        //TODO explain path + INSERT DSL
        Statement statement = select("TODO");

        assertEquals("EXPLAIN INSERT INTO tutorial (KEY, VALUE) " +
                "VALUES (\"baldwin\", {\"name\":\"Alex Baldwin\", \"type\":\"contact\"})", statement.toString());
    }

    @Test
    @Ignore("need EXPLAIN and DELETE dsl")
    public void test30() {
        //TODO explain path + DELETE DSL
        Statement statement = select("*");

        assertEquals("EXPLAIN DELETE FROM tutorial t USE KEYS \"baldwin\" RETURNING t", statement.toString());
    }

    @Test
    @Ignore("needs EXPLAIN and UPDATE dsl")
    public void test31() {
        //TODO explain path + UPDATE DML
        Statement statement = select("*");

        assertEquals("EXPLAIN UPDATE tutorial USE KEYS \"baldwin\" " +
                "SET type = \"actor\" RETURNING tutorial.type", statement.toString());
    }

    @Test
    public void test32() {
        Statement statement = select(count("*").as("product_count")).from("product");
        assertEquals("SELECT COUNT(*) AS product_count FROM product", statement.toString());
    }

    @Test
    public void test33() {
        Statement statement = select("*").from("product")
                .unnest("product.categories").as("cat")
                .where(lower("cat").in(JsonArray.from("golf")))
                .limit(10).offset(10);

        assertEquals("SELECT * FROM product " +
                "UNNEST product.categories AS cat " +
                "WHERE LOWER(cat) IN [\"golf\"] LIMIT 10 OFFSET 10", statement.toString());
    }

    @Test
    public void test34() {
        Statement statement = selectDistinct("categories").from("product")
                .unnest("product.categories").as("categories");

        assertEquals("SELECT DISTINCT categories FROM product " +
                "UNNEST product.categories AS categories", statement.toString());
    }

    @Test
    public void test35() {
        Statement statement = select("productId", "name").from("product")
                .where(lower("name").like(s("%cup%")));

        assertEquals("SELECT productId, name FROM product WHERE LOWER(name) LIKE \"%cup%\"", statement.toString());
    }

    @Test
    public void test36() {
        Statement statement = select("product").from("product")
                .unnest("product.categories").as("categories")
                .where(x("categories").eq(s("Appliances")));

        assertEquals("SELECT product FROM product UNNEST product.categories AS categories " +
                "WHERE categories = \"Appliances\"", statement.toString());
    }

    @Test
    public void test37() {
        Statement statement = select(
                x("product.name"),
                count("reviews").as("reviewCount"),
                round(avg("reviews.rating"), 1).as("AvgRating"),
                x("category"))
                .from("reviews").as("reviews")
                .join("product").as("product").onKeys("reviews.productId");

        assertEquals("SELECT product.name, COUNT(reviews) AS reviewCount, " +
                "ROUND(AVG(reviews.rating), 1) AS AvgRating, category " +
                "FROM reviews AS reviews " +
                "JOIN product AS product ON KEYS reviews.productId", statement.toString());
    }

    @Test
    public void test38() {
        Statement statement = select(x("product.name"), x("product.dateAdded"), sum("items.count").as("unitsSold"))
                .from("purchases").unnest("purchases.lineItems").as("items")
                .join("product").onKeys("items.product").groupBy("product")
                .orderBy(Sort.def("product.dateAdded"), Sort.desc("unitsSold"))
                .limit(10);

        assertEquals("SELECT product.name, product.dateAdded, SUM(items.count) AS unitsSold " +
                "FROM purchases UNNEST purchases.lineItems AS items " +
                "JOIN product ON KEYS items.product GROUP BY product " +
                "ORDER BY product.dateAdded, unitsSold DESC LIMIT 10", statement.toString());
    }

    @Test
    public void test39() {
        Statement statement = select("product.name", "product.unitPrice", "product.categories").from("product")
                .unnest("product.categories").as("categories")
                .where(x("categories").eq(s("Appliances"))
                                      .and(x("product.unitPrice").lt(6.99)));

        assertEquals("SELECT product.name, product.unitPrice, product.categories FROM product " +
                "UNNEST product.categories AS categories WHERE categories = \"Appliances\" AND product.unitPrice < 6.99",
                statement.toString());
    }

    @Test
    public void test40() {
        Statement statement = select(x("product.name"), sum("items.count").as("unitsSold")).from("purchases")
                .unnest("purchases.lineItems").as("items")
                .join("product").onKeys("items.product")
                .groupBy("product")
                .orderBy(Sort.desc("unitsSold")).limit(10);

        assertEquals("SELECT product.name, SUM(items.count) AS unitsSold FROM purchases " +
                "UNNEST purchases.lineItems AS items JOIN product ON KEYS items.product " +
                "GROUP BY product ORDER BY unitsSold DESC LIMIT 10", statement.toString());
    }

    @Test
    public void test41() {
        Statement statement = select(x("product.name"), round(avg("reviews.rating"), 1).as("avg_rating"))
                .from("reviews")
                .join("product").onKeys("reviews.productId")
                .groupBy("product")
                .orderBy(Sort.desc(avg("reviews.rating"))).limit(5);

        assertEquals("SELECT product.name, ROUND(AVG(reviews.rating), 1) AS avg_rating FROM reviews " +
                "JOIN product ON KEYS reviews.productId GROUP BY product " +
                "ORDER BY AVG(reviews.rating) DESC LIMIT 5", statement.toString());
    }

    @Test
    public void test42() {
        Statement statement = select("purchases", "product", "customer").from("purchases")
                .useKeysValues("purchase0")
                .unnest("purchases.lineItems").as("items")
                .join("product").onKeys("items.product")
                .join("customer").onKeys("purchases.customerId");

        assertEquals("SELECT purchases, product, customer FROM purchases USE KEYS \"purchase0\" " +
                "UNNEST purchases.lineItems AS items JOIN product ON KEYS items.product " +
                "JOIN customer ON KEYS purchases.customerId", statement.toString());
    }

    @Test
    public void test43() {
        Statement statement = select(x("customer.firstName"), x("customer.lastName"), x("customer.emailAddress"),
                sum("items.count").as("purchaseCount"),
                round(sum(x("product.unitPrice").multiply("items.count"))).as("totalSpent"))
                .from("purchases")
                .unnest("purchases.lineItems").as("items")
                .join("product").onKeys("items.product")
                .join("customer").onKeys("purchases.customerId")
                .groupBy("customer");

        assertEquals("SELECT customer.firstName, customer.lastName, customer.emailAddress, " +
                "SUM(items.count) AS purchaseCount, ROUND(SUM(product.unitPrice * items.count)) AS totalSpent " +
                "FROM purchases UNNEST purchases.lineItems AS items " +
                "JOIN product ON KEYS items.product JOIN customer ON KEYS purchases.customerId GROUP BY customer",
                statement.toString());
    }

    @Test
    public void test44() {
        Statement statement = select(count("customer").as("customerCount"), x("state")).from("customer")
                .groupBy("state").orderBy(Sort.desc("customerCount"));

        assertEquals("SELECT COUNT(customer) AS customerCount, state FROM customer " +
                "GROUP BY state ORDER BY customerCount DESC", statement.toString());
    }

    @Test
    public void test45() {
        Statement statement = select(count(distinct("purchases.customerId"))).from("purchases")
                .where(strToMillis("purchases.purchasedAt")
                        .between(strToMillis(s("2014-02-01"))
                                .and(strToMillis(s("2014-03-01")))));

        assertEquals("SELECT COUNT(DISTINCT purchases.customerId) FROM purchases " +
                "WHERE STR_TO_MILLIS(purchases.purchasedAt) BETWEEN STR_TO_MILLIS(\"2014-02-01\") " +
                "AND STR_TO_MILLIS(\"2014-03-01\")", statement.toString());
    }

    @Test
    public void test46() {
        Statement statement = select(x("product"), avg("reviews.rating").as("avgRating"),
                count("reviews").as("numReviews"))
                .from("product")
                .join("reviews").onKeys("product.reviewList")
                .groupBy("product").having(avg("reviews.rating").lt(1));

        assertEquals("SELECT product, AVG(reviews.rating) AS avgRating, COUNT(reviews) AS numReviews " +
                "FROM product JOIN reviews ON KEYS product.reviewList " +
                "GROUP BY product HAVING AVG(reviews.rating) < 1", statement.toString());
    }

    @Test
    public void test47() {
        Statement statement = select(substr("purchases.purchasedAt", 0, 7).as("month"),
                round(sum(x("product.unitPrice").multiply("items.count")).divide(1000000), 3).as("revenueMillion"))
                .from("purchases")
                .unnest("purchases.lineItems").as("items")
                .join("product").onKeys("items.product")
                .groupBy(substr("purchases.purchasedAt", 0, 7))
                .orderBy(Sort.def("month"));

        assertEquals("SELECT SUBSTR(purchases.purchasedAt, 0, 7) AS month, " +
                "ROUND(SUM(product.unitPrice * items.count) / 1000000, 3) AS revenueMillion " +
                "FROM purchases UNNEST purchases.lineItems AS items JOIN product ON KEYS items.product " +
                "GROUP BY SUBSTR(purchases.purchasedAt, 0, 7) " +
                "ORDER BY month", statement.toString());
    }

    @Test
    public void test48() {
        Statement statement = select("purchases.purchaseId", "l.product").from("purchases")
                .unnest("purchases.lineItems").as("l")
                .where(datePartStr("purchases.purchasedAt", month).eq(4)
                .and(datePartStr("purchases.purchasedAt", year).eq(2014))
                .and(sub(select("product.productId")
                                .from("product")
                                .useKeys("l.product")
                                .where(x("product.unitPrice").gt(500)))
                    .exists()
                ));

        assertEquals("SELECT purchases.purchaseId, l.product FROM purchases UNNEST purchases.lineItems AS l " +
                "WHERE DATE_PART_STR(purchases.purchasedAt, \"month\") = 4 " +
                "AND DATE_PART_STR(purchases.purchasedAt, \"year\") = 2014 " +
                "AND EXISTS (SELECT product.productId " +
                "FROM product USE KEYS l.product WHERE product.unitPrice > 500)", statement.toString());
    }

    @Test
    public void test49() {
        Statement statement = select("*").from("jungleville_inbox").limit(1);
        assertEquals("SELECT * FROM jungleville_inbox LIMIT 1", statement.toString());
    }

    @Test
    public void test50() {
        Statement statement = select("*").from("jungleville").as("`game-data`")
                .join("jungleville_stats").as("stats").onKeysValues("zid-jungle-stats-0001")
                .nest("jungleville_inbox").as("inbox").onKeysValues("zid-jungle-inbox-0001")
                .where(path(i("game-data"), "uuid").eq(s("zid-jungle-0001")));

        assertEquals("SELECT * FROM jungleville AS `game-data` JOIN jungleville_stats AS stats " +
                "ON KEYS \"zid-jungle-stats-0001\" NEST jungleville_inbox AS inbox ON KEYS \"zid-jungle-inbox-0001\" " +
                "WHERE `game-data`.uuid = \"zid-jungle-0001\"", statement.toString());
    }

    @Test
    public void test51() {
        Statement statement = select("player.name", "inbox.messages")
                .from("jungleville").as("player")
                .useKeysValues("zid-jungle-0001")
                .leftJoin("jungleville_inbox").as("inbox")
                .onKeys(s("zid-jungle-inbox-").concat(substr("player.uuid", 11)));

        assertEquals("SELECT player.name, inbox.messages FROM jungleville AS player USE KEYS \"zid-jungle-0001\" " +
                "LEFT JOIN jungleville_inbox AS inbox ON KEYS \"zid-jungle-inbox-\" || SUBSTR(player.uuid, 11)", statement.toString());
    }

    @Test
    public void test52() {
        Statement statement = select(x("stats.uuid").as("player"), x("hist.uuid").as("opponent"),
                sum(caseSearch().when(x("hist.result").eq(s("won"))).then(x(1)).elseReturn(x(0))).as("wins"),
                sum(caseSearch().when(x("hist.result").eq(s("lost"))).then(x(1)).elseReturn(x(0))).as("losses"))
                .from("jungleville_stats").as("stats").useKeysValues("zid-jungle-stats-0004")
                .unnest("stats.`pvp-hist`").as("hist")
                .groupBy("stats.uuid", "hist.uuid");

        assertEquals("SELECT stats.uuid AS player, hist.uuid AS opponent, " +
                "SUM(CASE WHEN hist.result = \"won\" THEN 1 ELSE 0 END) AS wins, " +
                "SUM(CASE WHEN hist.result = \"lost\" THEN 1 ELSE 0 END) AS losses " +
                "FROM jungleville_stats AS stats USE KEYS \"zid-jungle-stats-0004\" " +
                "UNNEST stats.`pvp-hist` AS hist GROUP BY stats.uuid, hist.uuid", statement.toString());
    }

    @Test
    public void test53() {
        Statement statement = select(x("player.name"), x("player.level"), x("stats.loadtime"),
                sum(caseSearch().when(x("hist.result").eq(s("won"))).then(x(1)).elseReturn(x(0))).as("wins"))
                .from("jungleville_stats").as("stats")
                .unnest("stats.`pvp-hist`").as("hist")
                .join("jungleville").as("player")
                .onKeys("stats.uuid")
                .groupBy("player", "stats");

        assertEquals("SELECT player.name, player.level, stats.loadtime, " +
                "SUM(CASE WHEN hist.result = \"won\" THEN 1 ELSE 0 END) AS wins " +
                "FROM jungleville_stats AS stats UNNEST stats.`pvp-hist` AS hist " +
                "JOIN jungleville AS player ON KEYS stats.uuid GROUP BY player, stats", statement.toString());
    }

    @Test
    public void test54() {
        Statement statement = select("jungleville.level", "friends")
                .from("jungleville").useKeysValues("zid-jungle-0002")
                .join("jungleville.friends").onKeys("jungleville.friends");

        assertEquals("SELECT jungleville.level, friends FROM jungleville USE KEYS \"zid-jungle-0002\" " +
                "JOIN jungleville.friends ON KEYS jungleville.friends", statement.toString());
    }

    @Test
    public void test55() {
        Statement statement = selectDistinctRaw("name").from("authors");

        assertEquals("SELECT DISTINCT RAW name FROM authors", statement.toString());
    }

    @Test
    public void test56() {
        Statement statement = selectDistinctRaw(x("name")).from("authors");

        assertEquals("SELECT DISTINCT RAW name FROM authors", statement.toString());
    }

    @Test
    public void test57() {
        Statement statement = selectDistinctRaw("books.authorName").from("A").as("books").join("A")
                .as("authors").useHash(HashSide.PROBE).on(x("books.authorName").eq(x("authors.name")))
                .where(x("books.type").eq(s("book")).and(x("authors.type").eq(s("author"))));

        assertEquals("SELECT DISTINCT RAW books.authorName FROM A AS books JOIN A AS authors USE HASH(PROBE) ON " +
                "books.authorName = authors.name WHERE books.type = \"book\" AND authors.type = \"author\"", statement.toString());
    }

    @Test
    public void test58() {
        Statement statement = selectDistinctRaw("books.authorName").from("A").as("books").join("A")
                .as("authors").useHash(HashSide.BUILD).on(x("books.authorName").eq(x("authors.name")))
                .where(x("books.type").eq(s("book")).and(x("authors.type").eq(s("author"))));

        assertEquals("SELECT DISTINCT RAW books.authorName FROM A AS books JOIN A AS authors USE HASH(BUILD) ON " +
                "books.authorName = authors.name WHERE books.type = \"book\" AND authors.type = \"author\"", statement.toString());
    }

    @Test
    public void test59() {
        Statement statement = selectDistinctRaw("books.authorName").from("A").as("books").join("A")
                .as("authors").useNestedLoop().on(x("books.authorName").eq(x("authors.name"))).where(x("books.type").eq(s("book")).and(x("authors.type").eq(s("author"))));

        assertEquals("SELECT DISTINCT RAW books.authorName FROM A AS books JOIN A AS authors USE NL ON " +
                "books.authorName = authors.name WHERE books.type = \"book\" AND authors.type = \"author\"", statement.toString());
    }
}
