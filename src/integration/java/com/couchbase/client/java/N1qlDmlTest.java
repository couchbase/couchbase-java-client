/*
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
package com.couchbase.client.java;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.couchbase.client.java.util.ClusterDependentTest;
import com.couchbase.client.java.util.features.CouchbaseFeature;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the functionality of various N1QL DML statements.
 *
 * @author Michael Nitschinger
 * @since 2.2.2
 */
public class N1qlDmlTest extends ClusterDependentTest {

    @BeforeClass
    public static void init() throws InterruptedException {
        Assume.assumeTrue(clusterManager().info().checkAvailable(CouchbaseFeature.N1QL));
    }

    @Test
    public void shouldInsertSingleDocumentViaSimple() {
        JsonObject value = JsonObject.create()
            .put("a", true)
            .put("b", 1234);

        String query = "INSERT INTO `" + bucketName() + "` (KEY, VALUE) VALUES ('insDoc1Simple', "
            +  value.toString() + ")";
        N1qlQueryResult result = bucket().query(N1qlQuery.simple(query));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        JsonDocument verified = bucket().get("insDoc1Simple");
        assertEquals(value, verified.content());
    }

    @Test
    public void shouldInsertMultipleDocumentsViaSimple() {
        JsonObject value = JsonObject.create()
            .put("a", true)
            .put("b", 1234);

        String query = "INSERT INTO `" + bucketName() + "` (KEY, VALUE) VALUES ('insDoc1Multi', "
            +  value.toString() + "), ('insDoc2Multi', " + value.toString() + ")";
        N1qlQueryResult result = bucket().query(N1qlQuery.simple(query));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        JsonDocument verified1 = bucket().get("insDoc1Multi");
        assertEquals(value, verified1.content());

        JsonDocument verified2 = bucket().get("insDoc2Multi");
        assertEquals(value, verified2.content());
    }

    @Test
    public void shouldInsertWithParameterized() {
        JsonObject value = JsonObject.create()
            .put("a", "foobar")
            .put("b", -12);

        String query = "INSERT INTO `" + bucketName() + "` (KEY, VALUE) VALUES ($key, $value)";
        N1qlQueryResult result = bucket().query(N1qlQuery.parameterized(
            query,
            JsonObject.create().put("key", "insParam1").put("value", value)
        ));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        JsonDocument verified1 = bucket().get("insParam1");
        assertEquals(value, verified1.content());
    }

    @Test
    public void shouldInsertWithReturning() {
        JsonObject value = JsonObject.create()
            .put("a", "foobar")
            .put("b", -12);

        String query = "INSERT INTO `" + bucketName() + "` (KEY, VALUE) VALUES ($key, $value) RETURNING *";
        N1qlQueryResult result = bucket().query(N1qlQuery.parameterized(
            query,
            JsonObject.create().put("key", "insRet1").put("value", value)
        ));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());

        List<N1qlQueryRow> rows = result.allRows();
        assertEquals(1, rows.size());
        assertEquals(value, rows.get(0).value().getObject(bucketName()));

        JsonDocument verified1 = bucket().get("insRet1");
        assertEquals(value, verified1.content());
    }

    @Test
    public void shouldFailOnDoubleInsert() {
        JsonObject value = JsonObject.create()
            .put("a", true)
            .put("b", JsonObject.empty().put("foo", "bar"));

        String query = "INSERT INTO `" + bucketName() + "` (KEY, VALUE) VALUES ('doubleIns1', "
            +  value.toString() + ")";
        N1qlQueryResult result = bucket().query(N1qlQuery.simple(query));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        JsonDocument verified = bucket().get("doubleIns1");
        assertEquals(value, verified.content());

        result = bucket().query(N1qlQuery.simple(query));

        assertFalse(result.finalSuccess());
        assertTrue(result.allRows().isEmpty());
        assertTrue(result.errors().get(0).getString("msg").contains("Duplicate Key"));
        assertEquals((Integer) 12009, result.errors().get(0).getInt("code"));
    }

    @Test
    public void shouldNotFailOnDoubleUpsert() {
        JsonObject value = JsonObject.create()
            .put("a", true)
            .put("b", JsonObject.empty().put("foo", "bar"));

        String query = "UPSERT INTO `" + bucketName() + "` (KEY, VALUE) VALUES ('doubleUps1', "
            +  value.toString() + ")";
        N1qlQueryResult result = bucket().query(N1qlQuery.simple(query));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        JsonDocument verified = bucket().get("doubleUps1");
        assertEquals(value, verified.content());

        value = JsonObject.create()
            .put("a", false)
            .put("b", JsonArray.from(1, 2, 3, 4));

        query = "UPSERT INTO `" + bucketName() + "` (KEY, VALUE) VALUES ('doubleUps1', "
            +  value.toString() + ")";
        result = bucket().query(N1qlQuery.simple(query));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        verified = bucket().get("doubleUps1");
        assertEquals(value, verified.content());
    }

    @Test
    public void shouldDeleteById() {
        String id = "n1qlDel1";
        JsonDocument stored = bucket().upsert(JsonDocument.create(id, JsonObject.create()));
        assertTrue(stored.cas() != 0);

        assertTrue(bucket().exists(id));

        String query = "DELETE FROM `" + bucketName() + "` USE KEYS ['" + id + "']";
        N1qlQueryResult result = bucket().query(N1qlQuery.simple(query));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        assertFalse(bucket().exists(id));
    }

    @Test
    @Ignore("Ignored until MB-16732 is resolved") // TODO: re-enable once MB-16732 is resolved
    public void shouldDeleteWithClause() throws Exception {
        String id1 = "n1qlDelW1";
        String id2 = "n1qlDelW2";

        JsonObject doc1 = JsonObject.create().put("type", "abc");
        JsonObject doc2 = JsonObject.create().put("type", "def");

        String query = "INSERT INTO `" + bucketName() + "` (KEY, VALUE) VALUES ('" + id1 + "', "
            +  doc1.toString() + "), ('" + id2 + "', " + doc2.toString() + ")";
        N1qlQueryResult result = bucket().query(N1qlQuery.simple(query));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        assertTrue(bucket().exists(id1));
        assertTrue(bucket().exists(id2));

        query = "DELETE FROM `" + bucketName() + "` WHERE type = 'def'";
        result = bucket().query(N1qlQuery.simple(query));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        assertTrue(bucket().exists(id1));
        assertFalse(bucket().exists(id2));
    }

    @Test
    public void shouldUpateById() {
        String id = "n1qlUpdate1";
        JsonDocument stored = bucket().upsert(JsonDocument.create(id, JsonObject.create()));
        assertTrue(stored.cas() != 0);

        assertTrue(bucket().exists(id));

        String query = "UPDATE `" + bucketName() + "` USE KEYS ['" + id + "'] SET updated = true";
        N1qlQueryResult result = bucket().query(N1qlQuery.simple(query));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        JsonDocument updated = bucket().get(id);
        assertEquals(true, updated.content().getBoolean("updated"));
    }

    @Test
    @Ignore("Ignored until MB-16732 is resolved") // TODO: re-enable once MB-16732 is resolved
    public void shouldUpdateWithClause() throws Exception {
        String id1 = "n1qlUpW1";
        String id2 = "n1qlUpW2";

        JsonObject doc1 = JsonObject.create().put("type", "abc");
        JsonObject doc2 = JsonObject.create().put("type", "def");

        String query = "UPSERT INTO `" + bucketName() + "` (KEY, VALUE) VALUES ('" + id1 + "', "
            +  doc1.toString() + "), ('" + id2 + "', " + doc2.toString() + ")";
        N1qlQueryResult result = bucket().query(N1qlQuery.simple(query));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        assertTrue(bucket().exists(id1));
        assertTrue(bucket().exists(id2));
        
        query = "UPDATE `" + bucketName() + "` SET type = 'ghi' WHERE type = 'abc'";
        result = bucket().query(N1qlQuery.simple(query));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        JsonDocument verify1 = bucket().get(id1);
        assertEquals("ghi", verify1.content().getString("type"));
        JsonDocument verify2 = bucket().get(id2);
        assertEquals("def", verify2.content().getString("type"));
    }



}
