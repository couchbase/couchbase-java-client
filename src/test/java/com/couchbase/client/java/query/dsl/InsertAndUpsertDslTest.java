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
