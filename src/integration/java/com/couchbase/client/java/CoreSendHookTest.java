/*
 * Copyright (c) 2017 Couchbase, Inc.
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

import com.couchbase.client.core.hooks.CouchbaseCoreSendHook;
import com.couchbase.client.core.lang.Tuple;
import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.message.CouchbaseRequest;
import com.couchbase.client.core.message.CouchbaseResponse;
import com.couchbase.client.core.message.kv.UpsertRequest;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.util.CouchbaseTestContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import rx.Observable;
import rx.functions.Action0;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

public class CoreSendHookTest {

    private static final CouchbaseLogger LOGGER =
        CouchbaseLoggerFactory.getInstance(CoreSendHookTest.class);

    private static CouchbaseTestContext ctx;

    private static final Map<CouchbaseRequest, Long> metrics = new ConcurrentHashMap<CouchbaseRequest, Long>();

    @BeforeClass
    public static void init() throws InterruptedException {
        assumeFalse(CouchbaseTestContext.isMockEnabled());
        assumeFalse(CouchbaseTestContext.isCi());

        ctx = CouchbaseTestContext.builder()
            .bucketName("CoreSendHookTest")
            .adhoc(true)
            .bucketQuota(100)
            .withEnv(DefaultCouchbaseEnvironment.builder().couchbaseCoreSendHook(new CouchbaseCoreSendHook() {
                @Override
                public Tuple2<CouchbaseRequest, Observable<CouchbaseResponse>> beforeSend(
                    final CouchbaseRequest originalRequest, Observable<CouchbaseResponse> originalResponse) {
                    if (originalRequest instanceof UpsertRequest) {
                        final long start = System.nanoTime();
                        Observable<CouchbaseResponse> intercepted = originalResponse.doOnCompleted(new Action0() {
                            @Override
                            public void call() {
                                long end = System.nanoTime();
                                metrics.put(originalRequest, end - start);
                            }
                        });
                        return Tuple.create(originalRequest, intercepted);
                    } else {
                        return Tuple.create(originalRequest, originalResponse);
                    }
                }
            }))
            .build();
    }

    @AfterClass
    public static void cleanup() {
        if (ctx != null) {
            ctx.destroyBucketAndDisconnect();
        }
    }

    @Test
    public void shouldRecordOperations() {
        assertTrue(metrics.isEmpty());
        ctx.bucket().upsert(JsonDocument.create("doc", JsonObject.empty()));
        assertEquals(1, metrics.size());

        for (Map.Entry<CouchbaseRequest, Long> entry : metrics.entrySet()) {
            assertTrue(entry.getKey() instanceof UpsertRequest);
            UpsertRequest request = (UpsertRequest) entry.getKey();

            assertNotNull(request.dispatchHostname());
            assertTrue(entry.getValue() > 0);
            assertTrue(request.opaque() > 0);
            LOGGER.info("Request {}, dispatched to {} took {}µs (opaque: {})", request,
                request.dispatchHostname(), TimeUnit.NANOSECONDS.toMicros(entry.getValue()), request.opaque());
        }
    }
}
