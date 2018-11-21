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
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.TemporaryFailureException;
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
import rx.functions.Func7;

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
    private static int count = 0;

    @BeforeClass
    public static void init() throws Exception {
        assumeFalse(CouchbaseTestContext.isMockEnabled());

        ctx = CouchbaseTestContext.builder()
                .adhoc(true)
                .bucketName("N1qlPrepared")
                .bucketQuota(100)
                .build()
                .ignoreIfNoN1ql()
        .ensurePrimaryIndex();

        if (ctx.rbacEnabled()) {
            executor = new N1qlQueryExecutor(ctx.cluster().core(), ctx.bucketName(), ctx.adminName(), ctx.adminPassword(), false);
        } else {
            executor = new N1qlQueryExecutor(ctx.cluster().core(), ctx.bucketName(), ctx.bucketPassword(), false);
        }


        final JsonObject jsonObject = JsonObject.create();
        for (int i=0; i < 10; i++) {
            jsonObject.put("field" + i,  UUID.randomUUID().toString());
        }
        List<String> keys = new ArrayList<String>();
        for (int i=0; i < 15000; i++) {
            keys.add("Key" + i);
        }

        List<JsonDocument> documents = Observable.from(keys)
                .flatMap(new Func1<String, Observable<JsonDocument>>() {
                    @Override
                    public Observable<JsonDocument> call(String key) {
                        return ctx.bucket().async().upsert(JsonDocument.create(key, jsonObject)) ;
                    }
                })
                .onErrorResumeNext(new Func1<Throwable, Observable<JsonDocument>>() {
                    @Override
                    public Observable<JsonDocument> call(Throwable throwable) {
                        if (throwable instanceof TemporaryFailureException) {
                            return Observable.empty();
                        } else {
                            return Observable.error(throwable);
                        }
                    }
                })
                .toList()
                .toBlocking()
                .single();

        count = documents.size();
        assertTrue(count >= 10000);
    }

    @AfterClass
    public static void cleanup() {
        if (ctx != null) {
            ctx.destroyBucketAndDisconnect();
        }
    }

    public static N1qlQueryResult query(N1qlQuery query) {
        return executor.execute(query, ctx.env(), ctx.env().queryTimeout(), TimeUnit.MILLISECONDS)
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
                                aqr.profileInfo().singleOrDefault(JsonObject.empty()),
                                aqr.status(),
                                aqr.finalSuccess().singleOrDefault(Boolean.FALSE),
                                new Func7<List<AsyncN1qlQueryRow>, Object, N1qlMetrics, List<JsonObject>, JsonObject, String, Boolean, N1qlQueryResult>() {
                                    @Override
                                    public N1qlQueryResult call(List<AsyncN1qlQueryRow> rows, Object signature,
                                                                N1qlMetrics info, List<JsonObject> errors, JsonObject profileInfo, String finalStatus, Boolean finalSuccess) {
                                        return new DefaultN1qlQueryResult(rows, signature, info, errors,  profileInfo, finalStatus, finalSuccess,
                                                parseSuccess, requestId, clientContextId);
                                    }
                                });
                    }
                }).toBlocking().singleOrDefault(null);
    }

    @Test
    public void testPreparedWithEncodedPlanDisabledExecutor() {
        N1qlQuery query = N1qlQuery.simple("SELECT * FROM `" + ctx.bucketName() + "` limit 2", N1qlParams.build().consistency(CONSISTENCY).adhoc(false));
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

    @Test
    public void testLongRunningPreparedQuery() {
        int fetzhSz = 10000;
        N1qlQuery query = N1qlQuery.simple("select * from `"+ ctx.bucketName() +"` limit " + fetzhSz, N1qlParams.build().adhoc(false));
        N1qlQueryResult result = ctx.bucket().query(query);
        System.out.println("Elapsed time:" + result.info().elapsedTime());
        assertEquals("Did not fetch all results", result.allRows().size(), fetzhSz);
    }
}