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
package com.couchbase.client.java.query.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.CouchbaseCore;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.error.QueryExecutionException;
import com.couchbase.client.java.query.AsyncN1qlQueryResult;
import com.couchbase.client.java.query.AsyncN1qlQueryRow;
import com.couchbase.client.java.query.DefaultAsyncN1qlQueryResult;
import com.couchbase.client.java.query.N1qlMetrics;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.PrepareStatement;
import com.couchbase.client.java.query.PreparedN1qlQuery;
import com.couchbase.client.java.query.PreparedPayload;
import com.couchbase.client.java.query.N1qlParams;
import com.couchbase.client.java.query.Select;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.util.LRUCache;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.mockito.internal.stubbing.answers.ReturnsElementsOf;
import rx.Observable;

/**
 * Tests the functionality of {@link N1qlQueryExecutor}.
 *
 * @author Simon Baslé
 * @since 2.2
 */
public class N1qlQueryExecutorTest {

    private static final CouchbaseEnvironment ENV = DefaultCouchbaseEnvironment.create();

    @AfterClass
    public static void tearDown() {
        ENV.shutdown();
    }

    @Test
    public void testPreparedStatementInCacheBypassesPreparation() throws Exception {
        LRUCache<String, PreparedPayload> cache = new LRUCache<String, PreparedPayload>(3);
        CouchbaseCore mockFacade = mock(CouchbaseCore.class);
        N1qlQueryExecutor executor = spy(new N1qlQueryExecutor(mockFacade, "default", "", "", cache, true));

        Statement st = Select.select("*");
        N1qlQuery q = N1qlQuery.simple(st, N1qlParams.build().adhoc(false));
        PreparedPayload payloadFromServer = new PreparedPayload(st, "server", "encodedPlan");
        PreparedPayload payloadInCache = new PreparedPayload(st, "cached", "encodedPlan");
        //put the statement in cache
        cache.put(st.toString(), payloadInCache);

        doReturn(Observable.empty()).when(executor).executeQuery(any(N1qlQuery.class), eq(ENV), any(Integer.class), any(TimeUnit.class));
        doReturn(Observable.just(payloadFromServer)).when(executor).prepare(any(Statement.class));
        doReturn(Observable.<AsyncN1qlQueryResult>empty()).when(executor)
                                                      .executePrepared(any(N1qlQuery.class), any(PreparedPayload.class),  eq(ENV), any(Integer.class), any(TimeUnit.class));

        executor.execute(q, ENV, 1, TimeUnit.SECONDS).toBlocking().firstOrDefault(null);

        verify(executor).dispatchPrepared(any(N1qlQuery.class), eq(ENV), any(Integer.class), any(TimeUnit.class));
        verify(executor, never()).prepare(any(Statement.class));
        verify(executor).executePrepared(q, payloadInCache, ENV, 1, TimeUnit.SECONDS);
        verify(executor, never()).executePrepared(q, payloadFromServer, ENV, 1, TimeUnit.SECONDS);
        verify(executor, never()).prepareAndExecute(any(N1qlQuery.class), eq(ENV), any(Integer.class), any(TimeUnit.class));
        verify(executor, never()).retryPrepareAndExecuteOnce(any(QueryExecutionException.class), any(N1qlQuery.class), eq(ENV), any(Integer.class), any(TimeUnit.class));
        assertEquals(1, cache.size());
    }

    @Test
    public void testPreparedStatementNotInCacheTriggersPreparation() throws Exception {
        LRUCache<String, PreparedPayload> cache = new LRUCache<String, PreparedPayload>(3);
        CouchbaseCore mockFacade = mock(CouchbaseCore.class);
        N1qlQueryExecutor executor = spy(new N1qlQueryExecutor(mockFacade, "default", "", "", cache, true));

        Statement st = Select.select("*");
        N1qlQuery q = N1qlQuery.simple(st, N1qlParams.build().adhoc(false));
        PreparedPayload payloadFromServer = new PreparedPayload(st, "server", "encodedPlan");

        doReturn(Observable.just(payloadFromServer)).when(executor).prepare(any(Statement.class));
        doReturn(Observable.<AsyncN1qlQueryResult>empty()).when(executor)
                                                      .executePrepared(any(N1qlQuery.class), any(PreparedPayload.class), eq(ENV), any(Integer.class), any(TimeUnit.class));

        assertEquals(0, cache.size());
        executor.execute(q, ENV, 1, TimeUnit.SECONDS).toBlocking().firstOrDefault(null);

        verify(executor).dispatchPrepared(any(N1qlQuery.class), eq(ENV), any(Integer.class), any(TimeUnit.class));
        verify(executor).prepare(any(Statement.class));
        verify(executor).executePrepared(q, payloadFromServer, ENV, 1, TimeUnit.SECONDS);
        verify(executor).prepareAndExecute(any(N1qlQuery.class), eq(ENV), any(Integer.class), any(TimeUnit.class));
        verify(executor, never()).retryPrepareAndExecuteOnce(any(QueryExecutionException.class), any(N1qlQuery.class), eq(ENV), any(Integer.class), any(TimeUnit.class));
        assertEquals(1, cache.size());

        //also check how the plan is used in a PreparedN1qlQuery
        PreparedPayload plan = cache.values().iterator().next();
        PreparedN1qlQuery planQuery = new PreparedN1qlQuery(plan, N1qlParams.build());
        JsonObject n1qlPlanQuery = planQuery.n1ql();
        assertEquals("server", plan.payload());
        assertEquals("server", n1qlPlanQuery.getString("prepared"));
        assertEquals("encodedPlan", n1qlPlanQuery.getString("encoded_plan"));
        assertFalse(n1qlPlanQuery.containsKey("statement"));
    }

    @Test
    public void testExtractionOfPayloadFromPrepareResponse() {
        LRUCache<String, PreparedPayload> cache = new LRUCache<String, PreparedPayload>(3);
        CouchbaseCore mockFacade = mock(CouchbaseCore.class);
        N1qlQueryExecutor executor = new N1qlQueryExecutor(mockFacade, "default", "", "", cache, true);

        JsonObject prepareResponse = JsonObject.create()
                .put("encoded_plan", "encoded123")
                .put("name", "UUID")
                .put("prepared", "SomeLongVersionOfThePlan")
                .put("text", "SELECT original FROM bucket");
        PrepareStatement prepared = PrepareStatement.prepare("SELECT something");
        PreparedPayload extracted = executor.extractPreparedPayloadFromResponse(prepared, prepareResponse);

        assertEquals("SELECT something", extracted.originalStatement().toString());
        assertEquals("UUID", extracted.payload());
        assertEquals("UUID", extracted.preparedName());
        assertEquals("encoded123", extracted.encodedPlan());

    }

    @Test
    public void testCachedPlanExecutionErrorTriggersRetry() throws Exception {
        LRUCache<String, PreparedPayload> cache = new LRUCache<String, PreparedPayload>(3);
        CouchbaseCore mockFacade = mock(CouchbaseCore.class);
        N1qlQueryExecutor executor = spy(new N1qlQueryExecutor(mockFacade, "default", "", "", cache, true));

        Statement st = Select.select("*");
        N1qlQuery q = N1qlQuery.simple(st, N1qlParams.build().adhoc(false));
        PreparedPayload payloadFromServer = new PreparedPayload(st, "server", "encodedPlan");
        PreparedPayload payloadFromCache = new PreparedPayload(st, "cache", "encodedPlan");
        AsyncN1qlQueryResult result4050 = new DefaultAsyncN1qlQueryResult(Observable.<AsyncN1qlQueryRow>empty(),
                Observable.empty(),
                Observable.<N1qlMetrics>empty(),
                Observable.just(JsonObject.create().put("code", 4050).put("msg", "notRelevant")),
                Observable.<JsonObject>empty(),
                Observable.just("fatal"),
                false, "req", "");

        cache.put(st.toString(), payloadFromCache);
        doReturn(Observable.just(payloadFromServer)).when(executor).prepare(any(Statement.class));
        doReturn(Observable.just(result4050)).when(executor).executeQuery(any(PreparedN1qlQuery.class), eq(ENV), any(Integer.class), any(TimeUnit.class));

        assertEquals(1, cache.size());
        assertEquals(payloadFromCache, cache.values().iterator().next());

        executor.execute(q, ENV, 1, TimeUnit.SECONDS).toBlocking().firstOrDefault(null);

        verify(executor).dispatchPrepared(any(N1qlQuery.class), eq(ENV), any(Integer.class), any(TimeUnit.class));
        verify(executor, times(1)).executePrepared(q, payloadFromCache, ENV, 1, TimeUnit.SECONDS);
        verify(executor, times(1)).executePrepared(q, payloadFromServer, ENV, 1, TimeUnit.SECONDS);
        verify(executor, times(1)).retryPrepareAndExecuteOnce(any(Throwable.class), any(N1qlQuery.class), eq(ENV), any(Integer.class), any(TimeUnit.class));
        verify(executor, times(1)).prepare(any(Statement.class));
        assertEquals(1, cache.size());
        assertEquals(payloadFromServer, cache.values().iterator().next());
    }

    @Test
    public void testUncachedPlanExecutionErrorTriggersRetry() throws Exception {
        LRUCache<String, PreparedPayload> cache = new LRUCache<String, PreparedPayload>(3);
        CouchbaseCore mockFacade = mock(CouchbaseCore.class);
        N1qlQueryExecutor executor = spy(new N1qlQueryExecutor(mockFacade, "default", "", "", cache, true));

        Statement st = Select.select("*");
        N1qlQuery q = N1qlQuery.simple(st, N1qlParams.build().adhoc(false));
        PreparedPayload payloadFromServer1 = new PreparedPayload(st, "badserver", "encodedPlan");
        PreparedPayload payloadFromServer2 = new PreparedPayload(st, "goodserver", "encodedPlan");
        List<Observable<PreparedPayload>> payloads = Arrays.asList(
                Observable.just(payloadFromServer1),
                Observable.just(payloadFromServer2));
        Observable<DefaultAsyncN1qlQueryResult> result4050 = Observable.just(new DefaultAsyncN1qlQueryResult(
                Observable.<AsyncN1qlQueryRow>empty(),
                Observable.empty(),
                Observable.<N1qlMetrics>empty(),
                Observable.just(JsonObject.create().put("code", 4050).put("msg", "notRelevant")),
                Observable.<JsonObject>empty(),
                Observable.just("fatal"),
                false, "req", ""));
        Observable<DefaultAsyncN1qlQueryResult> resultOk = Observable.just(new DefaultAsyncN1qlQueryResult(
                Observable.<AsyncN1qlQueryRow>empty(),
                Observable.empty(),
                Observable.<N1qlMetrics>empty(),
                Observable.<JsonObject>empty(),
                Observable.<JsonObject>empty(),
                Observable.just("success"),
                true, "req", ""));

        doAnswer(new ReturnsElementsOf(payloads)).when(executor).prepare(any(Statement.class));
        doAnswer(new ReturnsElementsOf(Arrays.asList(result4050, resultOk))).when(executor).executeQuery(
                any(PreparedN1qlQuery.class), eq(ENV), any(Integer.class), any(TimeUnit.class));

        assertEquals(0, cache.size());

        AsyncN1qlQueryResult result = executor.execute(q, ENV, 1, TimeUnit.SECONDS).toBlocking().firstOrDefault(null);
        List<JsonObject> errors = result.errors().toList().toBlocking().first();
        boolean success = result.finalSuccess().toBlocking().first();

        verify(executor).dispatchPrepared(any(N1qlQuery.class), eq(ENV), any(Integer.class), any(TimeUnit.class));
        verify(executor, times(1)).executePrepared(q, payloadFromServer1, ENV, 1, TimeUnit.SECONDS);
        verify(executor, times(1)).executePrepared(q, payloadFromServer2, ENV, 1, TimeUnit.SECONDS);
        verify(executor, times(1)).retryPrepareAndExecuteOnce(any(Throwable.class), any(N1qlQuery.class), eq(ENV), any(Integer.class), any(TimeUnit.class));
        verify(executor, times(2)).prepare(any(Statement.class));
        assertEquals(1, cache.size());
        assertEquals(payloadFromServer2, cache.values().iterator().next());
        assertTrue(success);
        assertEquals(0, errors.size());
    }


    @Test
    public void testUncachedPlanExecutionDoubleErrorTriggersRetryThenFails() throws Exception {
        LRUCache<String, PreparedPayload> cache = new LRUCache<String, PreparedPayload>(3);
        CouchbaseCore mockFacade = mock(CouchbaseCore.class);
        N1qlQueryExecutor executor = spy(new N1qlQueryExecutor(mockFacade, "default", "", "", cache, true));

        Statement st = Select.select("*");
        N1qlQuery q = N1qlQuery.simple(st, N1qlParams.build().adhoc(false));
        PreparedPayload payloadFromServer1 = new PreparedPayload(st, "badserver", "encodedPlan");
        PreparedPayload payloadFromServer2 = new PreparedPayload(st, "goodserver", "encodedPlan");
        List<Observable<PreparedPayload>> payloads = Arrays.asList(
                Observable.just(payloadFromServer1),
                Observable.just(payloadFromServer2));
        Observable<DefaultAsyncN1qlQueryResult> result4050 = Observable.just(new DefaultAsyncN1qlQueryResult(
                Observable.<AsyncN1qlQueryRow>empty(),
                Observable.empty(),
                Observable.<N1qlMetrics>empty(),
                Observable.just(JsonObject.create().put("code", 4050).put("msg", "notRelevant")),
                Observable.<JsonObject>empty(),
                Observable.just("fatal"),
                false, "req", ""));
        Observable<DefaultAsyncN1qlQueryResult> resultOk = Observable.just(new DefaultAsyncN1qlQueryResult(
                Observable.<AsyncN1qlQueryRow>empty(),
                Observable.empty(),
                Observable.<N1qlMetrics>empty(),
                Observable.<JsonObject>empty(),
                Observable.<JsonObject>empty(),
                Observable.just("completed"),
                true, "req", ""));

        doAnswer(new ReturnsElementsOf(
                payloads
        )).when(executor).prepare(any(Statement.class));
        doAnswer(new ReturnsElementsOf(Arrays.asList(
                result4050,
                result4050,
                resultOk
        ))).when(executor).executeQuery(any(PreparedN1qlQuery.class), eq(ENV), any(Integer.class), any(TimeUnit.class));

        assertEquals(0, cache.size());

        AsyncN1qlQueryResult result = executor.execute(q, ENV, 1, TimeUnit.SECONDS).toBlocking().firstOrDefault(null);
        List<JsonObject> errors = result.errors().toList().toBlocking().first();
        boolean success = result.finalSuccess().toBlocking().first();

        verify(executor).dispatchPrepared(any(N1qlQuery.class), eq(ENV), any(Integer.class), any(TimeUnit.class));
        verify(executor, times(1)).executePrepared(q, payloadFromServer1, ENV, 1, TimeUnit.SECONDS);
        verify(executor, times(1)).executePrepared(q, payloadFromServer2, ENV, 1, TimeUnit.SECONDS);
        verify(executor, times(1)).retryPrepareAndExecuteOnce(any(Throwable.class), any(N1qlQuery.class), eq(ENV), any(Integer.class), any(TimeUnit.class));
        verify(executor, times(2)).prepare(any(Statement.class));
        assertEquals(1, cache.size());
        assertEquals(payloadFromServer2, cache.values().iterator().next());
        assertFalse(success);
        assertEquals(1, errors.size());
        assertEquals(4050, errors.get(0).getInt("code").intValue());
    }

    private void testRetryCondition(int code, String msg, boolean retryExpected) throws Exception {
        LRUCache<String, PreparedPayload> cache = new LRUCache<String, PreparedPayload>(3);
        CouchbaseCore mockFacade = mock(CouchbaseCore.class);
        N1qlQueryExecutor executor = spy(new N1qlQueryExecutor(mockFacade, "default", "", "", cache, true));

        Statement st = Select.select("*");
        N1qlQuery q = N1qlQuery.simple(st, N1qlParams.build().adhoc(false));

        PreparedPayload payloadFromServer1 = new PreparedPayload(st, "badserver", "encodedPlan");
        PreparedPayload payloadFromServer2 = new PreparedPayload(st, "goodserver", "encodedPlan");
        List<Observable<PreparedPayload>> payloads = Arrays.asList(
                Observable.just(payloadFromServer1),
                Observable.just(payloadFromServer2));
        Observable<DefaultAsyncN1qlQueryResult> resultRetry = Observable.just(new DefaultAsyncN1qlQueryResult(
                Observable.<AsyncN1qlQueryRow>empty(),
                Observable.empty(),
                Observable.<N1qlMetrics>empty(),
                Observable.just(JsonObject.create().put("code", code).put("msg", msg)),
                Observable.<JsonObject>empty(),
                Observable.just("fatal"),
                false, "req", ""));
        Observable<DefaultAsyncN1qlQueryResult> resultOk = Observable.just(new DefaultAsyncN1qlQueryResult(
                Observable.<AsyncN1qlQueryRow>empty(),
                Observable.empty(),
                Observable.<N1qlMetrics>empty(),
                Observable.<JsonObject>empty(),
                Observable.<JsonObject>empty(),
                Observable.just("success"),
                true, "req", ""));

        doAnswer(new ReturnsElementsOf(
                payloads
        )).when(executor).prepare(any(Statement.class));

        doAnswer(new ReturnsElementsOf(Arrays.asList(
                resultRetry,
                resultOk
        ))).when(executor).executeQuery(any(PreparedN1qlQuery.class), eq(ENV), any(Integer.class), any(TimeUnit.class));

        assertEquals(0, cache.size());

        AsyncN1qlQueryResult result = executor.execute(q, ENV, 1, TimeUnit.SECONDS).toBlocking().firstOrDefault(null); //ok
        List<JsonObject> errors = result.errors().toList().toBlocking().first();
        boolean success = result.finalSuccess().toBlocking().first();

        if (retryExpected) {
            verify(executor, times(1)).retryPrepareAndExecuteOnce(any(Throwable.class), any(N1qlQuery.class), eq(ENV), any(Integer.class), any(TimeUnit.class));
            assertTrue(success);
            assertEquals(0, errors.size());
            assertEquals(1, cache.size());
            assertEquals(payloadFromServer2, cache.values().iterator().next());
        } else {
            verify(executor, never()).retryPrepareAndExecuteOnce(any(Throwable.class), any(N1qlQuery.class), eq(ENV), any(Integer.class), any(TimeUnit.class));
            assertFalse(success);
            assertEquals(1, errors.size());
            assertEquals(new Integer(code), errors.get(0).getInt("code"));
            assertEquals(msg, errors.get(0).getString("msg"));
            assertEquals(1, cache.size());
            assertEquals(payloadFromServer1, cache.values().iterator().next());
        }
    }

    @Test
    public void testRetryOn4050() throws Exception {
        testRetryCondition(4050, "notRelevant", true);
    }

    @Test
    public void testRetryOn4070() throws Exception {
        testRetryCondition(4070, "notRelevant", true);
    }

    @Test
    public void testRetryOn5000WithSpecificMessage() throws Exception {
        testRetryCondition(5000, "toto" + N1qlQueryExecutor.ERROR_5000_SPECIFIC_MESSAGE, true);
    }

    @Test
    public void testNoRetryOn5000WithRandomMessage() throws Exception {
        testRetryCondition(5000, "notRelevant", false);
    }
}