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
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.Update;
import com.couchbase.client.java.query.dsl.clause.UpdateForClause;
import com.couchbase.client.java.query.dsl.path.DefaultMutateLimitPath;
import com.couchbase.client.java.query.dsl.path.DefaultMutateWherePath;
import com.couchbase.client.java.query.dsl.path.DefaultReturningPath;
import com.couchbase.client.java.query.dsl.path.DefaultUpdateSetOrUnsetPath;
import com.couchbase.client.java.query.dsl.path.DefaultUpdateUsePath;
import org.junit.Test;

import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;
import static com.couchbase.client.java.query.dsl.clause.UpdateForClause.forIn;
import static com.couchbase.client.java.query.dsl.clause.UpdateForClause.forWithin;
import static com.couchbase.client.java.query.dsl.functions.StringFunctions.upper;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the Update Builder API.
 *
 * @author Michael Nitschinger
 * @since 2.2.3
 */
public class UpdateDslTest {

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
  public void shouldConvertUnset() {
    Statement statement = new DefaultUpdateSetOrUnsetPath(null)
      .unset("foo")
      .unset("bar", x("FOR a IN b END"));

    assertEquals("UNSET foo , bar FOR a IN b END", statement.toString());

    statement = new DefaultUpdateSetOrUnsetPath(null).unset("foo");
    assertEquals("UNSET foo", statement.toString());
  }

  @Test
  public void shouldConvertSet() {
    Statement statement = new DefaultUpdateSetOrUnsetPath(null)
      .set("foo", s("bar"))
      .set("what", true);

    assertEquals("SET foo = \"bar\" , what = TRUE", statement.toString());

    statement = new DefaultUpdateSetOrUnsetPath(null)
      .set("foo", s("bar"))
      .where(x("a").gt(5));

    assertEquals("SET foo = \"bar\" WHERE a > 5", statement.toString());

    statement = new DefaultUpdateSetOrUnsetPath(null)
      .set("foo", JsonObject.create().put("a", "b"))
      .where(x("a").gt(5));

    assertEquals("SET foo = {\"a\":\"b\"} WHERE a > 5", statement.toString());
  }

  @Test
  public void shouldConvertSetAndUnset() {
    Statement statement = new DefaultUpdateSetOrUnsetPath(null)
      .set("a", JsonObject.create().put("x", "y"))
      .set("e", true)
      .unset("b")
      .unset("c")
      .where(x("a").gt(5));

    assertEquals("SET a = {\"x\":\"y\"} , e = TRUE UNSET b , c WHERE a > 5", statement.toString());
  }

  @Test
  public void shouldConvertUse() {
    Statement statement = new DefaultUpdateUsePath(null)
      .useKeysValues("foo", "bar", "baz");
    assertEquals("USE KEYS [\"foo\",\"bar\",\"baz\"]", statement.toString());
  }

  @Test
  public void shouldConvertUseWithSetAndUnset() {
    Statement statement = new DefaultUpdateUsePath(null)
      .useKeysValues("foo", "bar", "baz")
      .set("e", true)
      .unset("b")
      .where(x("a").gt(5));

    assertEquals(
      "USE KEYS [\"foo\",\"bar\",\"baz\"] SET e = TRUE UNSET b WHERE a > 5",
      statement.toString()
    );
  }

  @Test
  public void shouldConvertFullUpdate() {
    Statement statement = Update
      .update("product")
      .useKeysValues("odwalla-juice1")
      .set("type", "product-juice")
      .returning("product.type");

    assertEquals(
      "UPDATE `product` USE KEYS \"odwalla-juice1\" SET type = \"product-juice\" RETURNING product.type",
      statement.toString()
    );

    statement = Update
      .update("product")
      .useKeysValues("odwalla-juice1")
      .unset("type")
      .returning("product.*");

    assertEquals(
      "UPDATE `product` USE KEYS \"odwalla-juice1\" UNSET type RETURNING product.*",
      statement.toString()
    );

    statement = Update
      .update(x("tutorial t"))
      .useKeysValues("dave")
      .unset("c.gender", x("FOR c IN children END"))
      .returning("t");

    assertEquals(
      "UPDATE tutorial t USE KEYS \"dave\" UNSET c.gender FOR c IN children END RETURNING t",
      statement.toString()
    );
  }

  @Test
  public void shouldConvertUpdateWithUpdateForClauseSingleIn() {
    String expected = "UPDATE `bucket1` USE KEYS \"abc123\" SET version.description = \"blabla\" FOR version IN versions" +
            " WHEN version.status = \"ACTIVE\" END RETURNING versions";

    Statement statement = Update
            .update(i("bucket1"))
            .useKeysValues("abc123")
            .set(x("version.description"), s("blabla"), forIn("version", "versions").when(x("version.status").eq(s("ACTIVE"))))
            .returning("versions");

    assertEquals(expected, statement.toString());
  }

  @Test
  public void shouldConvertUpdateWithUpdateForClauseSingleWithin() {
    String expected = "UPDATE `bucket1` USE KEYS \"abc123\" SET version.description = \"blabla\" FOR version WITHIN versions" +
            " WHEN version.status = \"ACTIVE\" END RETURNING versions";

    Statement statement = Update
            .update(i("bucket1"))
            .useKeysValues("abc123")
            .set(x("version.description"), s("blabla"), forWithin("version", "versions").when(x("version.status").eq(s("ACTIVE"))))
            .returning("versions");

    assertEquals(expected, statement.toString());
  }

  @Test
  public void shouldConvertUpdateWithUpdateForClauseBothInAndWithin() {
    String expected = "UPDATE `bucket1` USE KEYS \"abc123\" SET version.description = \"blabla\" FOR version IN versions" +
            ", version2 WITHIN altVersions WHEN version.status = \"ACTIVE\" END RETURNING versions";

    Statement statement = Update
            .update(i("bucket1"))
            .useKeysValues("abc123")
            .set(x("version.description"), s("blabla"), forIn("version", "versions")
                    .within("version2", "altVersions").when(x("version.status").eq(s("ACTIVE"))))
            .returning("versions");

    assertEquals(expected, statement.toString());
  }

  @Test
  public void shouldConvertUpdateWithUpdateForClauseNoCondition() {
    String expected = "UPDATE `bucket1` USE KEYS \"abc123\" SET version.description = \"blabla\" FOR version IN versions" +
            ", version2 WITHIN altVersions END RETURNING versions";

    Statement statement = Update
            .update(i("bucket1"))
            .useKeysValues("abc123")
            .set(x("version.description"), s("blabla"), forIn("version", "versions").within("version2", "altVersions").end())
            .returning("versions");

    assertEquals(expected, statement.toString());
  }

}
