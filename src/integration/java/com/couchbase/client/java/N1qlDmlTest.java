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
package com.couchbase.client.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlParams;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import com.couchbase.client.java.util.CouchbaseTestContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies the functionality of various N1QL DML statements.
 *
 * @author Michael Nitschinger
 * @since 2.2.2
 */
public class N1qlDmlTest {

    public static CouchbaseTestContext ctx;

    @BeforeClass
    public static void init() throws Exception {
        assumeFalse(CouchbaseTestContext.isMockEnabled());

        ctx = CouchbaseTestContext.builder()
                .bucketName("N1qlDml")
                .adhoc(true)
                .bucketQuota(100)
                .build()
            .ignoreIfNoN1ql()
            .ensurePrimaryIndex();
    }

    @AfterClass
    public static void cleanup() {
        if (ctx != null) {
            ctx.destroyBucketAndDisconnect();
        }
    }

    @Test
    public void shouldInsertSingleDocumentViaSimple() {
        JsonObject value = JsonObject.create()
            .put("a", true)
            .put("b", 1234);

        String query = "INSERT INTO `" + ctx.bucketName() + "` (KEY, VALUE) VALUES ('insDoc1Simple', "
            +  value.toString() + ")";
        N1qlQueryResult result = ctx.bucket().query(N1qlQuery.simple(query));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        JsonDocument verified = ctx.bucket().get("insDoc1Simple");
        assertEquals(value, verified.content());
    }

    @Test
    public void shouldInsertMultipleDocumentsViaSimple() {
        JsonObject value = JsonObject.create()
            .put("a", true)
            .put("b", 1234);

        String query = "INSERT INTO `" + ctx.bucketName() + "` (KEY, VALUE) VALUES ('insDoc1Multi', "
            +  value.toString() + "), ('insDoc2Multi', " + value.toString() + ")";
        N1qlQueryResult result = ctx.bucket().query(N1qlQuery.simple(query));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        JsonDocument verified1 = ctx.bucket().get("insDoc1Multi");
        assertEquals(value, verified1.content());

        JsonDocument verified2 = ctx.bucket().get("insDoc2Multi");
        assertEquals(value, verified2.content());
    }

    @Test
    public void shouldInsertWithParameterized() {
        JsonObject value = JsonObject.create()
            .put("a", "foobar")
            .put("b", -12);

        String query = "INSERT INTO `" + ctx.bucketName() + "` (KEY, VALUE) VALUES ($key, $value)";
        N1qlQueryResult result = ctx.bucket().query(N1qlQuery.parameterized(
            query,
            JsonObject.create().put("key", "insParam1").put("value", value)
        ));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        JsonDocument verified1 = ctx.bucket().get("insParam1");
        assertEquals(value, verified1.content());
    }

    @Test
    public void shouldInsertWithReturning() {
        JsonObject value = JsonObject.create()
            .put("a", "foobar")
            .put("b", -12);

        String query = "INSERT INTO `" + ctx.bucketName() + "` (KEY, VALUE) VALUES ($key, $value) RETURNING *";
        N1qlQueryResult result = ctx.bucket().query(N1qlQuery.parameterized(
                query,
                JsonObject.create().put("key", "insRet1").put("value", value)
        ));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());

        List<N1qlQueryRow> rows = result.allRows();
        assertEquals(1, rows.size());
        assertEquals(value, rows.get(0).value().getObject(ctx.bucketName()));

        JsonDocument verified1 = ctx.bucket().get("insRet1");
        assertEquals(value, verified1.content());
    }

    @Test
    public void shouldFailOnDoubleInsert() {
        JsonObject value = JsonObject.create()
            .put("a", true)
            .put("b", JsonObject.empty().put("foo", "bar"));

        String query = "INSERT INTO `" + ctx.bucketName() + "` (KEY, VALUE) VALUES ('doubleIns1', "
            +  value.toString() + ")";
        N1qlQueryResult result = ctx.bucket().query(N1qlQuery.simple(query));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        JsonDocument verified = ctx.bucket().get("doubleIns1");
        assertEquals(value, verified.content());

        result = ctx.bucket().query(N1qlQuery.simple(query));

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

        String query = "UPSERT INTO `" + ctx.bucketName() + "` (KEY, VALUE) VALUES ('doubleUps1', "
            +  value.toString() + ")";
        N1qlQueryResult result = ctx.bucket().query(N1qlQuery.simple(query));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        JsonDocument verified = ctx.bucket().get("doubleUps1");
        assertEquals(value, verified.content());

        value = JsonObject.create()
            .put("a", false)
            .put("b", JsonArray.from(1, 2, 3, 4));

        query = "UPSERT INTO `" + ctx.bucketName() + "` (KEY, VALUE) VALUES ('doubleUps1', "
            +  value.toString() + ")";
        result = ctx.bucket().query(N1qlQuery.simple(query));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        verified = ctx.bucket().get("doubleUps1");
        assertEquals(value, verified.content());
    }

    @Test
    public void shouldDeleteById() {
        String id = "n1qlDel1";
        JsonDocument stored = ctx.bucket().upsert(JsonDocument.create(id, JsonObject.create()));
        assertTrue(stored.cas() != 0);

        assertTrue(ctx.bucket().exists(id));

        String query = "DELETE FROM `" + ctx.bucketName() + "` USE KEYS ['" + id + "']";
        N1qlQueryResult result = ctx.bucket().query(N1qlQuery.simple(query));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        assertFalse(ctx.bucket().exists(id));
    }

    @Test
    public void shouldDeleteWithClause() throws Exception {
        String id1 = "n1qlDelW1";
        String id2 = "n1qlDelW2";

        JsonObject doc1 = JsonObject.create().put("type", "abc");
        JsonObject doc2 = JsonObject.create().put("type", "def");

        String query = "INSERT INTO `" + ctx.bucketName() + "` (KEY, VALUE) VALUES ('" + id1 + "', "
            +  doc1.toString() + "), ('" + id2 + "', " + doc2.toString() + ")";
        N1qlQueryResult result = ctx.bucket().query(N1qlQuery.simple(query));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        assertTrue(ctx.bucket().exists(id1));
        assertTrue(ctx.bucket().exists(id2));

        query = "DELETE FROM `" + ctx.bucketName() + "` WHERE type = 'def'";
        result = ctx.bucket().query(N1qlQuery.simple(query, N1qlParams.build().consistency(ScanConsistency.REQUEST_PLUS)));

        assertTrue(ctx.errorMsg("Could not DELETE FROM", result), result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        assertTrue(ctx.bucket().exists(id1));
        assertFalse(ctx.bucket().exists(id2));
    }

    @Test
    public void shouldUpateById() {
        String id = "n1qlUpdate1";
        JsonDocument stored = ctx.bucket().upsert(JsonDocument.create(id, JsonObject.create()));
        assertTrue(stored.cas() != 0);

        assertTrue(ctx.bucket().exists(id));

        String query = "UPDATE `" + ctx.bucketName() + "` USE KEYS ['" + id + "'] SET updated = true";
        N1qlQueryResult result = ctx.bucket().query(N1qlQuery.simple(query));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        JsonDocument updated = ctx.bucket().get(id);
        assertEquals(true, updated.content().getBoolean("updated"));
    }

    @Test
    public void shouldUpdateWithClause() throws Exception {
        String id1 = "n1qlUpW1";
        String id2 = "n1qlUpW2";

        JsonObject doc1 = JsonObject.create().put("type", "abc");
        JsonObject doc2 = JsonObject.create().put("type", "def");

        String query = "UPSERT INTO `" + ctx.bucketName() + "` (KEY, VALUE) VALUES ('" + id1 + "', "
            +  doc1.toString() + "), ('" + id2 + "', " + doc2.toString() + ")";
        N1qlQueryResult result = ctx.bucket().query(N1qlQuery.simple(query));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        assertTrue(ctx.bucket().exists(id1));
        assertTrue(ctx.bucket().exists(id2));

        query = "UPDATE `" + ctx.bucketName() + "` SET type = 'ghi' WHERE type = 'abc'";
        result = ctx.bucket().query(N1qlQuery.simple(query, N1qlParams.build().consistency(ScanConsistency.REQUEST_PLUS)));

        assertTrue(result.finalSuccess());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.allRows().isEmpty());

        JsonDocument verify1 = ctx.bucket().get(id1);
        assertEquals("ghi", verify1.content().getString("type"));
        JsonDocument verify2 = ctx.bucket().get(id2);
        assertEquals("def", verify2.content().getString("type"));
    }

}
