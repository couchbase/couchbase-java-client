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

import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.Merge;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.dsl.path.DefaultMergeDeletePath;
import com.couchbase.client.java.query.dsl.path.DefaultMergeInsertPath;
import com.couchbase.client.java.query.dsl.path.DefaultMergeKeyClausePath;
import com.couchbase.client.java.query.dsl.path.DefaultMergeSourcePath;
import com.couchbase.client.java.query.dsl.path.DefaultMergeUpdatePath;
import com.couchbase.client.java.query.dsl.path.DefaultMutateLimitPath;
import com.couchbase.client.java.query.dsl.path.DefaultReturningPath;
import com.couchbase.client.java.query.dsl.path.Path;
import org.junit.Test;

import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;
import static com.couchbase.client.java.query.dsl.functions.StringFunctions.upper;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the Merge Builder API.
 *
 * @author Michael Nitschinger
 * @since 2.2.3
 */
public class MergeDslTest {

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
  public void shouldConvertLimit() {
    Statement statement = new DefaultMutateLimitPath(null).limit(5);
    assertEquals("LIMIT 5", statement.toString());
  }

  @Test
  public void shouldConvertLimitWithReturning() {
    Statement statement = new DefaultMutateLimitPath(null)
      .limit(5)
      .returning("foo");
    assertEquals("LIMIT 5 RETURNING foo", statement.toString());
  }

  @Test
  public void shouldConvertMergeInsert() {
    Statement statement = new DefaultMergeInsertPath(null)
      .whenNotMatchedThenInsert(x(JsonObject.create().put("foo", "bar")));
    assertEquals("WHEN NOT MATCHED THEN INSERT {\"foo\":\"bar\"}", statement.toString());

    statement = new DefaultMergeInsertPath(null)
      .whenNotMatchedThenInsert(x(JsonObject.create().put("foo", "bar"))).where(x("a").gt(5));
    assertEquals("WHEN NOT MATCHED THEN INSERT {\"foo\":\"bar\"} WHERE a > 5", statement.toString());
  }

  @Test
  public void shouldConvertMergeInsertWithLimit() {
    Statement statement = new DefaultMergeInsertPath(null)
      .whenNotMatchedThenInsert(x(JsonObject.create().put("foo", "bar")))
      .limit(5);
    assertEquals("WHEN NOT MATCHED THEN INSERT {\"foo\":\"bar\"} LIMIT 5", statement.toString());
  }

  @Test
  public void shouldConvertMergeDelete() {
    Statement statement = new DefaultMergeDeletePath(null)
      .whenMatchedThenDelete().where(x("a").eq(s("b")));
    assertEquals("WHEN MATCHED THEN DELETE WHERE a = \"b\"", statement.toString());

    statement = new DefaultMergeDeletePath(null)
      .whenMatchedThenDelete();
    assertEquals("WHEN MATCHED THEN DELETE", statement.toString());
  }

  @Test
  public void shouldConvertMergeDeleteWithInsert() {
    Statement statement = new DefaultMergeDeletePath(null)
      .whenMatchedThenDelete().where(x("a").eq(s("b")))
      .whenNotMatchedThenInsert(x(JsonObject.create().put("foo", "bar")));

    assertEquals(
      "WHEN MATCHED THEN DELETE WHERE a = \"b\" WHEN NOT MATCHED THEN INSERT {\"foo\":\"bar\"}",
      statement.toString()
    );
  }

  @Test
  public void shouldConvertMergeUpdateWithSetAndUnset() {
    Statement statement = new DefaultMergeUpdatePath(null)
      .whenMatchedThenUpdate()
      .set("foo", "bar")
      .set("bar", 5);


    assertEquals(
      "WHEN MATCHED THEN UPDATE SET foo = \"bar\" , bar = 5",
      statement.toString()
    );

    statement = new DefaultMergeUpdatePath(null)
      .whenMatchedThenUpdate()
      .unset("foo")
      .unset("bar");

    assertEquals(
      "WHEN MATCHED THEN UPDATE UNSET foo , bar",
      statement.toString()
    );

    statement = new DefaultMergeUpdatePath(null)
      .whenMatchedThenUpdate()
      .set("foo", "bar")
      .unset("baz");

    assertEquals(
      "WHEN MATCHED THEN UPDATE SET foo = \"bar\" UNSET baz",
      statement.toString()
    );

    statement = new DefaultMergeUpdatePath(null)
      .whenMatchedThenUpdate()
      .set("foo", "bar")
      .set("bla", JsonObject.empty())
      .unset("baz")
      .unset("jup");

    assertEquals(
      "WHEN MATCHED THEN UPDATE SET foo = \"bar\" , bla = {} UNSET baz , jup",
      statement.toString()
    );
  }

  @Test
  public void shouldConvertMergeKeyClause() {
    Statement statement = new DefaultMergeKeyClausePath(null)
      .onKey("o.productId");
    assertEquals("ON KEY o.productId", statement.toString());

    statement = new DefaultMergeKeyClausePath(null)
      .onPrimaryKey("foobar");
    assertEquals("ON PRIMARY KEY foobar", statement.toString());

    statement = new DefaultMergeKeyClausePath(null)
      .onKey("o.productId")
      .whenMatchedThenUpdate()
      .set("p.lastSaleDate", x("o.orderDate"));
    assertEquals(
      "ON KEY o.productId WHEN MATCHED THEN UPDATE SET p.lastSaleDate = o.orderDate",
      statement.toString()
    );
  }

  @Test
  public void shouldConvertMergeSource() {
    Path path = new DefaultMergeSourcePath(null)
      .using(x("orders").as("o"));
    assertEquals("USING orders AS o", path.toString());

    Statement statement = new DefaultMergeSourcePath(null)
      .using(x("orders").as("o"))
      .onKey("o.productId");
    assertEquals("USING orders AS o ON KEY o.productId", statement.toString());
  }

  @Test
  public void shouldConvertFullMerge() {
    Statement statement = Merge
      .mergeInto(x("product p"))
      .using("orders o")
      .onKey("o.productId")
      .whenMatchedThenUpdate().set("p.lastSaleDate", x("o.orderDate"))
      .whenMatchedThenDelete().where(x("p.inventoryCount").lte(0));

    assertEquals(
      "MERGE INTO product p " +
        "USING orders o " +
        "ON KEY o.productId " +
        "WHEN MATCHED THEN UPDATE SET p.lastSaleDate = o.orderDate " +
        "WHEN MATCHED THEN DELETE WHERE p.inventoryCount <= 0",
      statement.toString()
    );

    statement = Merge
      .mergeInto(x("all_empts a"))
      .using(x("emps_deptb").as("b"))
      .onKey("b.empId")
      .whenMatchedThenUpdate()
        .set("a.depts", x("a.depts").add(1))
        .set("a.title", x("b.title").concat(s(", ")).concat("b.title"))
      .whenNotMatchedThenInsert(
        x("{ \"name\": b.name, \"title\": b.title, \"depts\": b.depts, \"empId\": " +
          "b.empId, \"dob\": b.dob }")
      );

    assertEquals(
      "MERGE INTO all_empts a USING emps_deptb AS b ON KEY b.empId " +
        "WHEN MATCHED THEN UPDATE SET a.depts = a.depts + 1 , " +
        "a.title = b.title || \", \" || b.title " +
        "WHEN NOT MATCHED THEN INSERT { \"name\": b.name, \"title\": b.title, \"depts\": " +
        "b.depts, \"empId\": b.empId, \"dob\": b.dob }",
      statement.toString()
    );
  }
}
