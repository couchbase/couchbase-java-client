/*
 * Copyright (c) 2018 Couchbase, Inc.
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
package com.couchbase.client.java.analytics;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.analytics.GenericAnalyticsRequest;
import com.couchbase.client.core.message.analytics.GenericAnalyticsResponse;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import rx.Observable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the functionality of the {@link AnalyticsQueryExecutor}.
 */
public class AnalyticsQueryExecutorTest {

    private static final String SYNTAX_ERROR = "{ \n" +
            "\t\t\"code\": 24000,\n" +
            "\t\t\"msg\": \"Syntax error: In line 1 >>select =1;<< Encountered \\\"=\\\" at column 8. \"\n" +
            "\t}";

    private static final String RETRYABLE_ERROR = "{ \n" +
            "\t\t\"code\": 21002,\n" +
            "\t\t\"msg\": \"\"\n" +
            "\t}";

    private static CouchbaseEnvironment ENV;

    @BeforeClass
    public static void setup() {
        ENV = DefaultCouchbaseEnvironment.create();
    }

    @AfterClass
    public static void cleanup() {
        ENV.shutdown();
    }

    @Test
    public void shouldCompleteSuccessfulQuery() {
        ClusterFacade core = mock(ClusterFacade.class);
        when(core.send(any(GenericAnalyticsRequest.class))).thenAnswer(new Answer<Observable<GenericAnalyticsResponse>>() {
            @Override
            public Observable<GenericAnalyticsResponse> answer(InvocationOnMock invocation) {
                return Observable.just(new GenericAnalyticsResponse(
                    Observable.<ByteBuf>empty(),
                    Observable.just(Unpooled.copiedBuffer("{\"foo\": true}", CharsetUtil.UTF_8)),
                    Observable.<ByteBuf>empty(),
                    Observable.just("success"),
                    Observable.<ByteBuf>empty(),
                    null,
                    null,
                    ResponseStatus.SUCCESS,
                    "requestid",
                    "clientid"
                ));
            }
        });
        AnalyticsQueryExecutor executor = new AnalyticsQueryExecutor(core, "bucket", "user", "pass");

        AnalyticsQuery query = new SimpleAnalyticsQuery("select *", AnalyticsParams.build());
        AsyncAnalyticsQueryResult result = executor.execute(query, ENV, 5, TimeUnit.SECONDS).toBlocking().single();

        assertTrue(result.parseSuccess());
        assertTrue(result.finalSuccess().toBlocking().single());
        assertEquals(JsonObject.create().put("foo", true), result.rows().toBlocking().first().value());
        assertEquals("requestid", result.requestId());
        assertEquals("clientid", result.clientContextId());
    }

    @Test
    public void shouldPropagateNonRetryableError() {
        ClusterFacade core = mock(ClusterFacade.class);
        when(core.send(any(GenericAnalyticsRequest.class))).thenAnswer(new Answer<Observable<GenericAnalyticsResponse>>() {
            @Override
            public Observable<GenericAnalyticsResponse> answer(InvocationOnMock invocation) {
                return Observable.just(new GenericAnalyticsResponse(
                        Observable.just(Unpooled.copiedBuffer(SYNTAX_ERROR, CharsetUtil.UTF_8)),
                        Observable.<ByteBuf>empty(),
                        Observable.<ByteBuf>empty(),
                        Observable.just("fatal"),
                        Observable.<ByteBuf>empty(),
                        null,
                        null,
                        ResponseStatus.INVALID_ARGUMENTS,
                        "requestid",
                        "clientid"
                ));
            }
        });
        AnalyticsQueryExecutor executor = new AnalyticsQueryExecutor(core, "bucket", "user", "pass");

        AnalyticsQuery query = new SimpleAnalyticsQuery("select =1", AnalyticsParams.build());
        AsyncAnalyticsQueryResult result = executor
            .execute(query, ENV, 5, TimeUnit.SECONDS)
            .toBlocking()
            .single();

        assertFalse(result.parseSuccess());
        assertFalse(result.finalSuccess().toBlocking().single());
        assertEquals("requestid", result.requestId());
        assertEquals("clientid", result.clientContextId());
        assertEquals(JsonObject.create()
            .put("msg", "Syntax error: In line 1 >>select =1;<< Encountered \"=\" at column 8. ")
            .put("code", 24000),
            result.errors().toBlocking().single()
        );
    }

    @Test
    public void shouldRetryRetryableErrorAndCompleteEventually() {
        ClusterFacade core = mock(ClusterFacade.class);
        final AtomicInteger retryCounter = new AtomicInteger(0);
        when(core.send(any(GenericAnalyticsRequest.class))).thenAnswer(new Answer<Observable<GenericAnalyticsResponse>>() {
            @Override
            public Observable<GenericAnalyticsResponse> answer(InvocationOnMock invocation) {
                if (retryCounter.incrementAndGet() > 5) {
                    return Observable.just(new GenericAnalyticsResponse(
                            Observable.<ByteBuf>empty(),
                            Observable.just(Unpooled.copiedBuffer("{\"foo\": true}", CharsetUtil.UTF_8)),
                            Observable.<ByteBuf>empty(),
                            Observable.just("success"),
                            Observable.<ByteBuf>empty(),
                            null,
                            null,
                            ResponseStatus.SUCCESS,
                            "requestid",
                            "clientid"
                    ));
                } else {
                    return Observable.just(new GenericAnalyticsResponse(
                            Observable.just(Unpooled.copiedBuffer(RETRYABLE_ERROR, CharsetUtil.UTF_8)),
                            Observable.<ByteBuf>empty(),
                            Observable.<ByteBuf>empty(),
                            Observable.just("fatal"),
                            Observable.<ByteBuf>empty(),
                            null,
                            null,
                            ResponseStatus.FAILURE,
                            "requestid",
                            "clientid"
                    ));
                }
            }
        });
        AnalyticsQueryExecutor executor = new AnalyticsQueryExecutor(core, "bucket", "user", "pass");

        AnalyticsQuery query = new SimpleAnalyticsQuery("select *", AnalyticsParams.build());
        AsyncAnalyticsQueryResult result = executor.execute(query, ENV, 5, TimeUnit.SECONDS).toBlocking().single();

        assertTrue(result.parseSuccess());
        assertTrue(result.finalSuccess().toBlocking().single());
        assertEquals(JsonObject.create().put("foo", true), result.rows().toBlocking().first().value());
        assertEquals("requestid", result.requestId());
        assertEquals("clientid", result.clientContextId());
    }

    @Test
    public void shouldRetryRetryableErrorAndPropagateErrorIfNeeded() {
        ClusterFacade core = mock(ClusterFacade.class);
        when(core.send(any(GenericAnalyticsRequest.class))).thenAnswer(new Answer<Observable<GenericAnalyticsResponse>>() {
            @Override
            public Observable<GenericAnalyticsResponse> answer(InvocationOnMock invocation) {
                return Observable.just(new GenericAnalyticsResponse(
                    Observable.just(Unpooled.copiedBuffer(RETRYABLE_ERROR, CharsetUtil.UTF_8)),
                    Observable.<ByteBuf>empty(),
                    Observable.<ByteBuf>empty(),
                    Observable.just("fatal"),
                    Observable.<ByteBuf>empty(),
                    null,
                    null,
                    ResponseStatus.FAILURE,
                    "requestid",
                    "clientid"
                ));
            }
        });
        AnalyticsQueryExecutor executor = new AnalyticsQueryExecutor(core, "bucket", "user", "pass");

        AnalyticsQuery query = new SimpleAnalyticsQuery("blarb", AnalyticsParams.build());
        AsyncAnalyticsQueryResult result = executor
            .execute(query, ENV, 5, TimeUnit.SECONDS)
            .toBlocking()
            .single();

        assertFalse(result.parseSuccess());
        assertFalse(result.finalSuccess().toBlocking().single());
        assertEquals("requestid", result.requestId());
        assertEquals("clientid", result.clientContextId());
        assertEquals(JsonObject.create()
                    .put("msg", "")
                    .put("code", 21002),
            result.errors().toBlocking().single()
        );
    }

}