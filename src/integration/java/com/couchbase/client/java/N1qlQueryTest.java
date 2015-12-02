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

import static com.couchbase.client.java.query.Index.createPrimaryIndex;
import static com.couchbase.client.java.query.Select.select;
import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.x;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlParams;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import com.couchbase.client.java.util.CouchbaseTestContext;
import com.couchbase.client.java.util.features.Version;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests of the N1QL Query features.
 *
 * @author Simon Baslé
 * @since 2.1
 */
public class N1qlQueryTest {

    private static CouchbaseTestContext ctx;

    private static final ScanConsistency CONSISTENCY = ScanConsistency.REQUEST_PLUS;
    private static final N1qlParams WITH_CONSISTENCY = N1qlParams.build().consistency(CONSISTENCY);

    @BeforeClass
    public static void init() throws InterruptedException {
        ctx = CouchbaseTestContext.builder()
                .bucketName("N1qlQuery")
                .adhoc(true)
                .bucketQuota(100)
                .build()
                .ignoreIfNoN1ql()
        .ensurePrimaryIndex();

        ctx.bucket().upsert(JsonDocument.create("test1", JsonObject.create().put("item", "value")));
        ctx.bucket().upsert(JsonDocument.create("test2", JsonObject.create().put("item", 123)));
    }

    @AfterClass
    public static void cleanup() {
        ctx.destroyBucketAndDisconnect();
    }

    @Test
    public void shouldAlreadyHaveCreatedIndex() {
        N1qlQueryResult indexResult = ctx.bucket().query(
                N1qlQuery.simple(createPrimaryIndex().on(ctx.bucketName()), WITH_CONSISTENCY));
        assertFalse(indexResult.finalSuccess());
        assertEquals(0, indexResult.allRows().size());
        assertNotNull(indexResult.info());
        //having two calls to errors() here validates that there's not a reference to the async stream
        //each time the method is called.
        assertEquals(1, indexResult.errors().size());
        assertEquals("GSI CreatePrimaryIndex() - cause: Index #primary already exist.",
                indexResult.errors().get(0).getString("msg"));
    }

    @Test
    public void shouldHaveRequestId() {
        N1qlQueryResult result = ctx.bucket().query(N1qlQuery.simple("SELECT * FROM `" + ctx.bucketName() + "` LIMIT 3",
                N1qlParams.build().withContextId(null).consistency(CONSISTENCY)));
        assertNotNull(result);
        assertTrue(result.parseSuccess());
        assertTrue(result.finalSuccess());
        assertNotNull(result.info());
        assertNotNull(result.allRows());
        assertNotNull(result.errors());
        assertTrue(result.errors().isEmpty());

        assertEquals("", result.clientContextId());
        assertNotNull(result.requestId());
        assertTrue(result.requestId().length() > 0);

        assertFalse(result.allRows().isEmpty());
    }

    @Test
    public void shouldHaveRequestIdAndContextId() {
        N1qlQuery query = N1qlQuery.simple("SELECT * FROM `" + ctx.bucketName() + "` LIMIT 3",
                N1qlParams.build().withContextId("TEST").consistency(CONSISTENCY));

        N1qlQueryResult result = ctx.bucket().query(query);
        assertNotNull(result);
        assertTrue(result.parseSuccess());
        assertTrue(result.finalSuccess());
        assertNotNull(result.info());
        assertNotNull(result.allRows());
        assertNotNull(result.errors());
        assertTrue(result.errors().toString(), result.errors().isEmpty());

        assertEquals("TEST", result.clientContextId());
        assertNotNull(result.requestId());
        assertTrue(result.requestId().length() > 0);

        assertFalse(result.allRows().isEmpty());
    }

    @Test
    public void shouldHaveRequestIdAndTruncatedContextId() {
        String contextIdMoreThan64Bytes = "123456789012345678901234567890123456789012345678901234567890☃BCD";
        String contextIdTruncatedExpected = new String(Arrays.copyOf(contextIdMoreThan64Bytes.getBytes(), 64));

        N1qlQuery query = N1qlQuery.simple("SELECT * FROM `" + ctx.bucketName() + "` LIMIT 3",
                N1qlParams.build().withContextId(contextIdMoreThan64Bytes).consistency(CONSISTENCY));
        N1qlQueryResult result = ctx.bucket().query(query);
        JsonObject params = JsonObject.create();
        query.params().injectParams(params);

        assertEquals(contextIdMoreThan64Bytes, params.getString("client_context_id"));
        assertNotNull(result);
        assertTrue(result.parseSuccess());
        assertTrue(result.finalSuccess());
        assertNotNull(result.info());
        assertNotNull(result.allRows());
        assertNotNull(result.errors());
        assertTrue(result.errors().isEmpty());

        assertEquals(contextIdTruncatedExpected, result.clientContextId());
        assertNotNull(result.requestId());
        assertTrue(result.requestId().length() > 0);

        assertFalse(result.allRows().isEmpty());
    }

    @Test
    public void shouldHaveSignature() {
        N1qlQuery query = N1qlQuery.simple("SELECT * FROM `" + ctx.bucketName() + "` LIMIT 3", WITH_CONSISTENCY);
        N1qlQueryResult result = ctx.bucket().query(query);

        assertNotNull(result);
        assertTrue(result.parseSuccess());
        assertTrue(result.finalSuccess());
        assertNotNull(result.info());
        assertNotNull(result.signature());
        assertNotNull(result.allRows());
        assertNotNull(result.errors());
        assertTrue(result.errors().isEmpty());

        assertEquals(JsonObject.create().put("*", "*"), result.signature());

        assertFalse(result.allRows().isEmpty());
    }

    @Test
    public void testNotAdhocPopulatesCache() {
        Statement statement = select(x("*")).from(i(ctx.bucketName())).limit(10);
        N1qlQuery query = N1qlQuery.simple(statement, N1qlParams.build().adhoc(false));
        ctx.bucket().invalidateQueryCache();
        N1qlQueryResult response = ctx.bucket().query(query);
        N1qlQueryResult responseFromCache = ctx.bucket().query(query);

        assertTrue(response.finalSuccess());
        assertTrue(responseFromCache.finalSuccess());
        assertEquals(1, ctx.bucket().invalidateQueryCache());
    }

    @Test
    public void testAdhocDoesntPopulateCache() {
        Statement statement = select(x("*")).from(i(ctx.bucketName())).limit(10);
        N1qlQuery query = N1qlQuery.simple(statement, N1qlParams.build().adhoc(true));

        ctx.bucket().invalidateQueryCache();
        N1qlQueryResult response = ctx.bucket().query(query);
        N1qlQueryResult secondResponse = ctx.bucket().query(query);

        assertTrue(response.finalSuccess());
        assertTrue(secondResponse.finalSuccess());
        assertEquals("query cache was unexpectedly populated", 0, ctx.bucket().invalidateQueryCache());
    }

    @Test
    public void testPreparedSumWorks() {
        ctx.ignoreIfClusterUnder(new Version(4, 1, 0));

        String statement = "SELECT sum(c1) FROM `" + ctx.bucketName() + "`";
        N1qlQuery query = N1qlQuery.simple(statement, N1qlParams.build().adhoc(false));

        ctx.bucket().invalidateQueryCache();
        N1qlQueryResult response = ctx.bucket().query(query);
        N1qlQueryResult secondResponse = ctx.bucket().query(query);

        assertTrue(response.finalSuccess());
        assertTrue(secondResponse.finalSuccess());
        assertEquals(1, ctx.bucket().invalidateQueryCache());
    }

    @Test
    public void testPreparedWithPositionalPlaceholdersExecute() {
        String statement = "SELECT * FROM `" + ctx.bucketName() + "` WHERE item = $1";
        N1qlQuery query = N1qlQuery.parameterized(statement, JsonArray.from("value"), N1qlParams.build().adhoc(false));
        N1qlQuery query2 = N1qlQuery.parameterized(statement, JsonArray.from(123), N1qlParams.build().adhoc(false));

        ctx.bucket().invalidateQueryCache();
        N1qlQueryResult response = ctx.bucket().query(query);
        N1qlQueryResult secondResponse = ctx.bucket().query(query2);

        assertTrue(response.finalSuccess());
        assertTrue(secondResponse.finalSuccess());
        assertEquals(1, ctx.bucket().invalidateQueryCache());

        assertEquals(1, response.allRows().size());
        assertEquals(1, secondResponse.allRows().size());
    }

    @Test
    public void testPreparedWithNamedPlaceholdersExecute() {
        String statement = "SELECT * FROM `" + ctx.bucketName() + "` WHERE item = $item";
        N1qlQuery query = N1qlQuery
                .parameterized(statement, JsonObject.create().put("item", "value"), N1qlParams.build().adhoc(false));
        N1qlQuery query2 = N1qlQuery.parameterized(statement, JsonObject.create().put("item", 123), N1qlParams.build().adhoc(false));

        ctx.bucket().invalidateQueryCache();
        N1qlQueryResult response = ctx.bucket().query(query);
        N1qlQueryResult secondResponse = ctx.bucket().query(query2);

        assertTrue(response.finalSuccess());
        assertTrue(secondResponse.finalSuccess());
        assertEquals(1, ctx.bucket().invalidateQueryCache());

        assertEquals(1, response.allRows().size());
        assertEquals(1, secondResponse.allRows().size());
    }
}
