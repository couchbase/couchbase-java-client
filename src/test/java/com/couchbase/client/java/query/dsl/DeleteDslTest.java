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

import com.couchbase.client.java.query.Delete;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.dsl.path.DefaultDeleteUsePath;
import com.couchbase.client.java.query.dsl.path.DefaultMutateLimitPath;
import com.couchbase.client.java.query.dsl.path.DefaultMutateWherePath;
import com.couchbase.client.java.query.dsl.path.DefaultReturningPath;
import org.junit.Test;

import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;
import static com.couchbase.client.java.query.dsl.functions.StringFunctions.upper;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the Delete Builder API.
 *
 * @author Michael Nitschinger
 * @since 2.2.3
 */
public class DeleteDslTest {

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
  public void shouldConvertWhere() {
    Statement statement = new DefaultMutateWherePath(null)
      .where("a = b");
    assertEquals("WHERE a = b", statement.toString());
  }

  @Test
  public void shouldConvertWhereWithLimit() {
    Statement statement = new DefaultMutateWherePath(null)
      .where(x("foo").gt(5))
      .limit(8);
    assertEquals("WHERE foo > 5 LIMIT 8", statement.toString());
  }

  @Test
  public void shouldConvertUse() {
    Statement statement = new DefaultDeleteUsePath(null)
      .useKeysValues("foo", "bar", "baz");
    assertEquals("USE KEYS [\"foo\",\"bar\",\"baz\"]", statement.toString());
  }

  @Test
  public void shouldConvertUseWithWhere() {
    Statement statement = new DefaultDeleteUsePath(null)
      .useKeysValues("foo", "bar", "baz")
      .where(x("name").eq(s("bla")));
    assertEquals("USE KEYS [\"foo\",\"bar\",\"baz\"] WHERE name = \"bla\"", statement.toString());
  }

  @Test
  public void shouldConvertFullDelete() {
    Statement statement = Delete
      .deleteFrom("default")
      .useKeysValues("foo", "bar", "baz")
      .where(x("name").like(s("A%")))
      .limit(10);

    assertEquals(
      "DELETE FROM `default` USE KEYS [\"foo\",\"bar\",\"baz\"] WHERE name LIKE \"A%\" LIMIT 10",
      statement.toString()
    );

    statement = Delete
      .deleteFromCurrentBucket();
    assertEquals("DELETE FROM #CURRENT_BUCKET#", statement.toString());

    statement = Delete
      .deleteFrom(i("travel-sample"))
      .where(x("age").gt("70"));

    assertEquals("DELETE FROM `travel-sample` WHERE age > 70", statement.toString());
  }

}
