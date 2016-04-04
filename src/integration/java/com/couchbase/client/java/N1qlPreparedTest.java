/*
 * Copyright (C) 2016 Couchbase, Inc.
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

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.AsyncN1qlQueryResult;
import com.couchbase.client.java.query.AsyncN1qlQueryRow;
import com.couchbase.client.java.query.DefaultN1qlQueryResult;
import com.couchbase.client.java.query.N1qlMetrics;
import com.couchbase.client.java.query.N1qlParams;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import com.couchbase.client.java.query.core.N1qlQueryExecutor;
import com.couchbase.client.java.util.CouchbaseTestContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func6;

/**
 * Integration tests of the N1QL Query features.
 *
 * @author Simon Baslé
 * @since 2.1
 */
public class N1qlPreparedTest {

    private static CouchbaseTestContext ctx;

    private static final ScanConsistency CONSISTENCY = ScanConsistency.REQUEST_PLUS;
    private static N1qlQueryExecutor executor;

    @BeforeClass
    public static void init() throws InterruptedException {
        ctx = CouchbaseTestContext.builder()
                .adhoc(true)
                .bucketQuota(100)
                .bucketName("queryPrepared")
                .build()
                .ignoreIfNoN1ql()
        .ensurePrimaryIndex();

        executor = new N1qlQueryExecutor(ctx.cluster().core(), ctx.bucketName(), ctx.bucketPassword(), false);

        ctx.bucket().upsert(JsonDocument.create("test1", JsonObject.create().put("item", "value")));
        ctx.bucket().upsert(JsonDocument.create("test2", JsonObject.create().put("item", 123)));
    }

    @AfterClass
    public static void cleanup() {
        ctx.destroyBucketAndDisconnect();
    }

    public static N1qlQueryResult query(N1qlQuery query) {
        return executor.execute(query)
                .flatMap(new Func1<AsyncN1qlQueryResult, Observable<N1qlQueryResult>>() {
                @Override
                public Observable<N1qlQueryResult> call(AsyncN1qlQueryResult aqr) {
                    final boolean parseSuccess = aqr.parseSuccess();
                    final String requestId = aqr.requestId();
                    final String clientContextId = aqr.clientContextId();

                    return Observable.zip(aqr.rows().toList(),
                        aqr.signature().singleOrDefault(JsonObject.empty()),
                        aqr.info().singleOrDefault(N1qlMetrics.EMPTY_METRICS),
                        aqr.errors().toList(),
                        aqr.status(),
                        aqr.finalSuccess().singleOrDefault(Boolean.FALSE),
                        new Func6<List<AsyncN1qlQueryRow>, Object, N1qlMetrics, List<JsonObject>, String, Boolean, N1qlQueryResult>() {
                            @Override
                            public N1qlQueryResult call(List<AsyncN1qlQueryRow> rows, Object signature,
                                                        N1qlMetrics info, List<JsonObject> errors, String finalStatus, Boolean finalSuccess) {
                                return new DefaultN1qlQueryResult(rows, signature, info, errors, finalStatus, finalSuccess,
                                    parseSuccess, requestId, clientContextId);
                            }
                        });
                }
            }).toBlocking().singleOrDefault(null);
    }

    @Test
    public void testPreparedWithEncodedPlanDisabledExecutor() {
        N1qlQuery query = N1qlQuery.simple("SELECT * FROM `" + ctx.bucketName() + "`", N1qlParams.build().consistency(CONSISTENCY).adhoc(false));
        N1qlQueryResult result = query(query); //this uses the executor with encodedPlan forcefully disabled
        List<N1qlQueryRow> list = result.allRows();
        List<JsonObject> errors = result.errors();
        assertEquals("error during first iteration: " + errors, Collections.emptyList(), errors);
        assertEquals("result set too small during first iteration", 2, list.size());
        System.out.println("Prepare and execute: " + result.info().executionTime());

        result = ctx.bucket().query(query);
        list = result.allRows();
        errors = result.errors();
        assertEquals("error during second iteration: " + errors, Collections.emptyList(), errors);
        assertEquals("result set too small during second iteration", 2, list.size());
    }
}
