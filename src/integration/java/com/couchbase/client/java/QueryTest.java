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
import static org.junit.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.NamedPreparedStatementException;
import com.couchbase.client.java.query.PrepareStatement;
import com.couchbase.client.java.query.PreparedPayload;
import com.couchbase.client.java.query.PreparedQuery;
import com.couchbase.client.java.query.Query;
import com.couchbase.client.java.query.QueryParams;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.query.QueryRow;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import com.couchbase.client.java.util.ClusterDependentTest;
import com.couchbase.client.java.util.features.CouchbaseFeature;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests of the N1QL Query features.
 *
 * @author Simon Baslé
 * @since 2.1
 */
public class QueryTest extends ClusterDependentTest {

    private static final ScanConsistency CONSISTENCY = ScanConsistency.REQUEST_PLUS;
    private static final QueryParams WITH_CONSISTENCY = QueryParams.build().consistency(CONSISTENCY);

    @BeforeClass
    public static void init() throws InterruptedException {
        Assume.assumeTrue( //skip tests unless...
                clusterManager().info().checkAvailable(CouchbaseFeature.N1QL) //...version >= 3.5.0 (packaged)
                        || env().queryEnabled()); //... or forced in environment by user

        Thread.sleep(1500);//attempt to avoid GSI "indexer rollback" error after flush
        bucket().upsert(JsonDocument.create("test1", JsonObject.create().put("item", "value")));
        bucket().upsert(JsonDocument.create("test2", JsonObject.create().put("item", 123)));

        bucket().query(Query.simple("CREATE PRIMARY INDEX ON `" + bucketName() + "`"));
    }

    @Test
    public void shouldAlreadyHaveCreatedIndex() {
        QueryResult indexResult = bucket().query(Query.simple(createPrimaryIndex().on(bucketName()), WITH_CONSISTENCY));
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
        QueryResult result = bucket().query(Query.simple("SELECT * FROM `" + bucketName() + "` LIMIT 3",
                QueryParams.build().withContextId(null).consistency(CONSISTENCY)));
        assertNotNull(result);
        assertTrue(result.parseSuccess());
        assertTrue(result.finalSuccess());
        assertNotNull(result.info());
        assertNotNull(result.allRows());
        assertNotNull(result.errors());
        assertFalse(result.allRows().isEmpty());
        assertTrue(result.errors().isEmpty());

        assertEquals("", result.clientContextId());
        assertNotNull(result.requestId());
        assertTrue(result.requestId().length() > 0);
    }

    @Test
    public void shouldHaveRequestIdAndContextId() {
        Query query = Query.simple("SELECT * FROM `" + bucketName() + "` LIMIT 3",
                QueryParams.build().withContextId("TEST").consistency(CONSISTENCY));

        QueryResult result = bucket().query(query);
        assertNotNull(result);
        assertTrue(result.parseSuccess());
        assertTrue(result.finalSuccess());
        assertNotNull(result.info());
        assertNotNull(result.allRows());
        assertNotNull(result.errors());
        assertTrue(result.errors().toString(), result.errors().isEmpty());
        assertFalse(result.allRows().isEmpty());

        assertEquals("TEST", result.clientContextId());
        assertNotNull(result.requestId());
        assertTrue(result.requestId().length() > 0);
    }

    @Test
    public void shouldHaveRequestIdAndTruncatedContextId() {
        String contextIdMoreThan64Bytes = "123456789012345678901234567890123456789012345678901234567890☃BCD";
        String contextIdTruncatedExpected = new String(Arrays.copyOf(contextIdMoreThan64Bytes.getBytes(), 64));

        Query query = Query.simple("SELECT * FROM `" + bucketName() + "` LIMIT 3",
                QueryParams.build().withContextId(contextIdMoreThan64Bytes).consistency(CONSISTENCY));
        QueryResult result = bucket().query(query);
        JsonObject params = JsonObject.create();
        query.params().injectParams(params);

        assertEquals(contextIdMoreThan64Bytes, params.getString("client_context_id"));
        assertNotNull(result);
        assertTrue(result.parseSuccess());
        assertTrue(result.finalSuccess());
        assertNotNull(result.info());
        assertNotNull(result.allRows());
        assertNotNull(result.errors());
        assertFalse(result.allRows().isEmpty());
        assertTrue(result.errors().isEmpty());

        assertEquals(contextIdTruncatedExpected, result.clientContextId());
        assertNotNull(result.requestId());
        assertTrue(result.requestId().length() > 0);
    }

    @Test
    public void shouldHaveSignature() {
        Query query = Query.simple("SELECT * FROM `" + bucketName() + "` LIMIT 3", WITH_CONSISTENCY);
        QueryResult result = bucket().query(query);

        assertNotNull(result);
        assertTrue(result.parseSuccess());
        assertTrue(result.finalSuccess());
        assertNotNull(result.info());
        assertNotNull(result.signature());
        assertNotNull(result.allRows());
        assertNotNull(result.errors());
        assertFalse(result.allRows().isEmpty());
        assertTrue(result.errors().isEmpty());

        assertEquals(JsonObject.create().put("*", "*"), result.signature());
    }

    @Test
    public void shouldProduceAndExecutePlan() {
        String preparedName = "testPreparedNamed";

        Statement statement = select(x("*")).from(i(bucketName())).where(x("item").eq(x("$1")));
        PrepareStatement prepareStatement = PrepareStatement.prepare(statement, preparedName);
        PreparedPayload payload = bucket().prepare(prepareStatement);
        assertNotNull(bucket().get("test2"));

        assertNotNull(payload);
        assertNotNull(payload.originalStatement());
        assertNotNull(payload.preparedName());
        assertEquals(statement.toString(), payload.originalStatement().toString());
        assertEquals(preparedName, payload.preparedName());

        PreparedQuery preparedQuery = Query.prepared(payload,
                JsonArray.from(123),
                QueryParams.build().withContextId("TEST").consistency(CONSISTENCY));
        QueryResult response = bucket().query(preparedQuery);
        assertTrue(response.errors().toString(), response.finalSuccess());
        List<QueryRow> rows = response.allRows();
        assertEquals("TEST", response.clientContextId());
        assertEquals(1, rows.size());
        assertTrue(rows.get(0).value().toString().contains("123"));
    }

    @Test
    public void shouldFailToExecuteUnknownNamedPreparedStatement() {
        SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMddHHmmss");
        String preparedName = "testPreparedNamed" + sdf.format(new Date());

        Statement statement = select(x("*")).from(i(bucketName())).where(x("item").eq(x("$1")));
        PrepareStatement prepareStatement = PrepareStatement.prepare(statement, preparedName);
        PreparedPayload payload = new PreparedPayload(prepareStatement, preparedName);
        PreparedQuery preparedQuery = Query.prepared(payload,
                JsonArray.from(123),
                QueryParams.build().withContextId("TEST").consistency(CONSISTENCY));
        try {
            QueryResult response = bucket().query(preparedQuery);
            fail("Expected NamedPreparedStatementException, got: " + response.allRows().toString() + ", errors: "
                + response.errors().toString());
        } catch (NamedPreparedStatementException e) {
            //success
        }
    }
}
