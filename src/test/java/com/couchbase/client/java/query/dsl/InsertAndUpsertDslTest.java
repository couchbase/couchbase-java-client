/**
 * Copyright (C) 2015 Couchbase, Inc.
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
import com.couchbase.client.java.query.Insert;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.Upsert;
import com.couchbase.client.java.query.dsl.path.DefaultInitialInsertPath;
import com.couchbase.client.java.query.dsl.path.DefaultReturningPath;
import org.junit.Test;

import static com.couchbase.client.java.query.Select.select;
import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;
import static com.couchbase.client.java.query.dsl.functions.StringFunctions.upper;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the Insert Builder API.
 *
 * @author Michael Nitschinger
 * @since 2.2.3
 */
public class InsertAndUpsertDslTest {

  @Test
  public void shouldConvertRegularReturning() {
    Statement statement = new DefaultReturningPath(null).returning("foobar");
    assertEquals("RETURNING foobar", statement.toString());
  }

  @Test
  public void shouldConvertRawReturning() {
    Statement statement = new DefaultReturningPath(null).returningRaw("foobar");
    assertEquals("RETURNING RAW foobar", statement.toString());
  }

  @Test
  public void shouldConvertElementReturning() {
    Statement statement = new DefaultReturningPath(null).returningElement("foobar");
    assertEquals("RETURNING ELEMENT foobar", statement.toString());

    statement = new DefaultReturningPath(null).returningElement(upper("bla"));
    assertEquals("RETURNING ELEMENT UPPER(bla)", statement.toString());
  }

  @Test
  public void shouldConvertValues() {
    Statement statement = new DefaultInitialInsertPath(null)
      .values("fooid", JsonObject.empty());

    assertEquals("VALUES (\"fooid\", {})", statement.toString());

    statement = new DefaultInitialInsertPath(null)
      .values("fooid", JsonObject.empty())
      .values("barid", JsonObject.create().put("foo", true));

    assertEquals(
      "VALUES (\"fooid\", {}) , (\"barid\", {\"foo\":true})",
      statement.toString()
    );

    statement = new DefaultInitialInsertPath(null)
      .values(s("foo").concat(s("bar")), JsonArray.from(1, 2, 3, 4))
      .values(upper(s("bla")), true);

    assertEquals(
      "VALUES (\"foo\" || \"bar\", [1,2,3,4]) , (UPPER(\"bla\"), TRUE)",
      statement.toString()
    );
  }

  @Test
  public void shouldConvertValuesWithReturning() {
    Statement statement = new DefaultInitialInsertPath(null)
      .values("user", JsonObject.create().put("fname", "michael").put("age", 27))
      .returning("fname");

    assertEquals(
      "VALUES (\"user\", {\"fname\":\"michael\",\"age\":27}) RETURNING fname",
      statement.toString()
    );
  }

  @Test
  public void shouldConvertFullInsertIntoValues() {
    Statement statement = Insert
      .insertInto("default")
      .values("user", JsonObject.create().put("fname", "michael").put("age", 27))
      .values("doc2", true)
      .returning("fname");

    assertEquals(
      "INSERT INTO `default` VALUES (\"user\", {\"fname\":\"michael\"," +
        "\"age\":27}) , (\"doc2\", TRUE) RETURNING fname",
      statement.toString()
    );
  }

  @Test
  public void shouldConvertFullInsertIntoSelect() {
    Statement statement = Insert
      .insertInto(i("beer-sample"))
      .select("code", select("`beer-sample`.*").from(i("beer-sample")).limit(1));

    assertEquals(
      "INSERT INTO `beer-sample` (KEY code) SELECT `beer-sample`.* FROM `beer-sample` LIMIT 1",
      statement.toString()
    );

    statement = Insert
      .insertInto(i("beer-sample"))
      .select(
        "code",
        x(JsonObject.create().put("c", "city").put("n", "name")),
        select("`beer-sample`.*").from(i("beer-sample")).limit(1)
      );

    assertEquals(
      "INSERT INTO `beer-sample` (KEY code, VALUE {\"n\":\"name\",\"c\":\"city\"}) SELECT " +
        "`beer-sample`.* FROM `beer-sample` LIMIT 1",
      statement.toString()
    );
  }


  @Test
  public void shouldConvertFullUpsertIntoValues() {
    Statement statement = Upsert
      .upsertInto("default")
      .values("user", JsonObject.create().put("fname", "michael").put("age", 27))
      .values("doc2", true)
      .returning("fname");

    assertEquals(
      "UPSERT INTO `default` VALUES (\"user\", {\"fname\":\"michael\"," +
        "\"age\":27}) , (\"doc2\", TRUE) RETURNING fname",
      statement.toString()
    );
  }

  @Test
  public void shouldConvertFullUpsertIntoSelect() {
    Statement statement = Upsert
      .upsertInto(i("beer-sample"))
      .select("code", select("`beer-sample`.*").from(i("beer-sample")).limit(1));

    assertEquals(
      "UPSERT INTO `beer-sample` (KEY code) SELECT `beer-sample`.* FROM `beer-sample` LIMIT 1",
      statement.toString()
    );

    statement = Upsert
      .upsertInto(i("beer-sample"))
      .select(
        "code",
        x(JsonObject.create().put("c", "city").put("n", "name")),
        select("`beer-sample`.*").from(i("beer-sample")).limit(1)
      );

    assertEquals(
      "UPSERT INTO `beer-sample` (KEY code, VALUE {\"n\":\"name\",\"c\":\"city\"}) SELECT " +
        "`beer-sample`.* FROM `beer-sample` LIMIT 1",
      statement.toString()
    );
  }

}
