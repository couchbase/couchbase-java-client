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

import static com.couchbase.client.java.query.Index.createPrimaryIndex;
import static com.couchbase.client.java.query.Select.select;
import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.x;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import java.lang.reflect.Field;
import java.util.Arrays;

import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.*;
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
    public static void init() throws Exception {
        assumeFalse(CouchbaseTestContext.isMockEnabled());

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
        if (ctx != null) {
            ctx.destroyBucketAndDisconnect();
        }
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
        assertThat(indexResult.errors().get(0).getString("msg"),
                containsString("GSI CreatePrimaryIndex() - cause: Index #primary already exist"));
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

        assertNotNull(result.clientContextId());
        assertFalse(result.clientContextId().isEmpty());
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

    @Test
    public void shouldSelectFromCurrentBucket() {
        N1qlQuery query = N1qlQuery.simple(
          select("*").fromCurrentBucket().limit(3),
          WITH_CONSISTENCY
        );

        N1qlQueryResult result = ctx.bucket().query(query);
        assertTrue(result.allRows().size() > 0);
        assertTrue(result.errors().isEmpty());
        assertTrue(result.finalSuccess());
    }

    @Test
    public void shouldDisableMetrics() {
        N1qlQuery query = N1qlQuery.simple(
            select("*").fromCurrentBucket().limit(1),
            N1qlParams.build().disableMetrics(true)
        );

        N1qlQueryResult result = ctx.bucket().query(query);
        assertEquals(N1qlMetrics.EMPTY_METRICS, result.info());
    }

    @Test
    public void shouldUseRawParams() {
        N1qlQuery query = N1qlQuery.simple(
            select("*").fromCurrentBucket().limit(1),
            N1qlParams.build()
                .rawParam("metrics", false)
                .rawParam("client_context_id", "somecustomID")
        );

        N1qlQueryResult result = ctx.bucket().query(query);
        assertEquals(N1qlMetrics.EMPTY_METRICS, result.info());
        assertEquals("somecustomID", result.clientContextId());
    }

    @Test
    public void shouldSelectByteArrayEagerlyAndJsonObjectLazily() throws NoSuchFieldException, IllegalAccessException {
        N1qlQuery query = N1qlQuery.simple(select("*").from(i(ctx.bucketName())).limit(3), WITH_CONSISTENCY);

        N1qlQueryResult result = ctx.bucket().query(query);
        assertTrue(result.allRows().size() > 0);
        assertTrue(result.errors().isEmpty());
        assertTrue(result.finalSuccess());

        for (N1qlQueryRow row : result) {
            assertNotNull(row.byteValue());

            //sync row is based on async
            Field asyncRowField = row.getClass().getDeclaredField("asyncRow");
            asyncRowField.setAccessible(true);
            DefaultAsyncN1qlQueryRow asyncRow = (DefaultAsyncN1qlQueryRow) asyncRowField.get(row);
            assertNotNull(asyncRow);

            //async row lazily initializes the JsonObject value, so initially null
            Field valueField = asyncRow.getClass().getDeclaredField("value");
            valueField.setAccessible(true);
            Object valueBeforeGetterCall = valueField.get(asyncRow);
            assertNull(valueBeforeGetterCall);

            //calling the getter lazily initializes
            assertNotNull(row.value());
            assertEquals(new String(row.byteValue(), CharsetUtil.UTF_8).replaceAll("\\s", "")
                    , row.value().toString().replaceAll("\\s", ""));
        }
    }

    @Test
    public void shouldWorkWithPrettyFalse() {
        ctx.ignoreIfClusterUnder(Version.parseVersion("4.5.1"));
        N1qlQuery query = N1qlQuery.simple(
            select("*").fromCurrentBucket().limit(1),
            N1qlParams.build().pretty(false).consistency(CONSISTENCY)
        );

        N1qlQueryResult result = ctx.bucket().query(query);
        assertEquals(1, result.allRows().size());
        assertTrue(result.parseSuccess());
        assertTrue(result.finalSuccess());
        assertNotNull(result.info());
        assertNotNull(result.allRows());
        assertNotNull(result.errors());
    }

    @Test
    public void shouldWorkWithProfileInfoEnabled() {
        ctx.ignoreIfClusterUnder(Version.parseVersion("4.5.1"));
        N1qlQuery query = N1qlQuery.simple(
                select("*").fromCurrentBucket().limit(1),
                N1qlParams.build().profile(N1qlProfile.TIMINGS).consistency(CONSISTENCY)
        );

        N1qlQueryResult result = ctx.bucket().query(query);
        assertEquals(1, result.allRows().size());
        assertTrue(result.parseSuccess());
        assertTrue(result.finalSuccess());
        assertNotNull(result.info());
        assertNotNull(result.allRows());
        assertNotNull(result.errors());
        assertTrue(result.profileInfo().size() > 0);
    }
}
