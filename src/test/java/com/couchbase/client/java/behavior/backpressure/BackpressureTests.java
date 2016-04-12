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
package com.couchbase.client.java.behavior.backpressure;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.couchbase.client.core.BackpressureException;
import com.couchbase.client.core.CouchbaseCore;
import com.couchbase.client.core.endpoint.kv.KeyValueStatus;
import com.couchbase.client.core.message.CouchbaseResponse;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.GetRequest;
import com.couchbase.client.core.message.kv.GetResponse;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.CouchbaseAsyncBucket;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.StringDocument;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.transcoder.Transcoder;
import com.couchbase.client.java.transcoder.TranscoderUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import rx.Observable;
import rx.functions.Func1;
import rx.observers.TestSubscriber;
import rx.subjects.Subject;

/**
 * Test the behavior of the SDK in backpressure-related scenarios.
 * Can be used as an example of how to handle such scenarios.
 *
 * @author Simon Baslé
 */
public class BackpressureTests {

    private static final int MAX_CAPACITY = 20; //size of queue after which the requests are rejected with BackpressureException
    private static final int MAX_CONCURRENT = 18; //maximum amount of parallel requested get in the flatmap
    private static final int RANGE = 100; //total number of requests made
    private static final int LATENCY = 50; //in milliseconds, delay after which the core responds


    private static final Func1<StringDocument, String> EXTRACT_CONTENT = new Func1<StringDocument, String>() {
        @Override
        public String call(StringDocument stringDocument) {
            return stringDocument.content();
        }
    };

    @Test
    public void testBulkPatternWithoutConcurrentFlatMapThrowsBackpressureException() {
        final AtomicLong counter = new AtomicLong(0L);
        final AtomicLong queued = new AtomicLong(0L);
        final AtomicBoolean overflowed = new AtomicBoolean(false);

        CouchbaseCore core = createMock(counter, queued, overflowed);
        final AsyncBucket bucket = new CouchbaseAsyncBucket(core, DefaultCouchbaseEnvironment.create(), "test", "",
                Collections.<Transcoder<? extends Document, ?>>emptyList());
        final Func1<Integer, Observable<StringDocument>> intToAsyncGet = new Func1<Integer, Observable<StringDocument>>() {
            @Override
            public Observable<StringDocument> call(Integer i) {
                return bucket.get("key" + i, StringDocument.class);
            }
        };

        Observable<String> bulkGet = Observable.range(1, RANGE)
                .flatMap(intToAsyncGet)
                .map(EXTRACT_CONTENT);

        TestSubscriber<String> subscriber = new TestSubscriber<String>();
        bulkGet.subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        System.out.printf("Sent %d requests, had %d still in queue", counter.longValue(),
                queued.longValue());
        subscriber.assertError(BackpressureException.class);

        assertEquals(MAX_CAPACITY, queued.longValue());
        verify(core, times(counter.intValue())).send(any(GetRequest.class));
        verifyNoMoreInteractions(core);
    }

    @Test
    public void testBulkPatternWithMaxConcurrentFlatMapControlsFlow() {
        final AtomicLong counter = new AtomicLong(0L);
        final AtomicLong queued = new AtomicLong(0L);
        final AtomicBoolean overflowed = new AtomicBoolean(false);

        CouchbaseCore core = createMock(counter, queued, overflowed);
        final AsyncBucket bucket = new CouchbaseAsyncBucket(core, DefaultCouchbaseEnvironment.create(), "test", "",
                Collections.<Transcoder<? extends Document, ?>>emptyList());
        final Func1<Integer, Observable<StringDocument>> intToAsyncGet = new Func1<Integer, Observable<StringDocument>>() {
            @Override
            public Observable<StringDocument> call(Integer i) {
                return bucket.get("key" + i, StringDocument.class);
            }
        };

        Observable<String> bulkGetFlowControl = Observable.range(1, RANGE)
                .flatMap(intToAsyncGet, MAX_CONCURRENT)
                .map(EXTRACT_CONTENT);

        TestSubscriber<String> subscriber = new TestSubscriber<String>();
        bulkGetFlowControl.subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        System.out.printf("Sent %d requests, had %d still in queue", counter.longValue(),
                queued.longValue());
        subscriber.assertNoErrors();
        subscriber.assertCompleted();
        subscriber.assertValueCount(RANGE);

        Assert.assertEquals(RANGE, counter.longValue());
        Assert.assertEquals(0, queued.longValue());
        verify(core, times(RANGE)).send(any(GetRequest.class));
        verifyNoMoreInteractions(core);
    }

    /**
     * Creates the mock that simulates the ring buffer and netty io.
     */
    private CouchbaseCore createMock(final AtomicLong counter, final AtomicLong queued, final AtomicBoolean overflowed) {
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(MAX_CAPACITY);
        CouchbaseCore core = mock(CouchbaseCore.class);

        when(core.send(any(GetRequest.class))).thenAnswer(new Answer<Observable<CouchbaseResponse>>() {
            @Override
            public Observable<CouchbaseResponse> answer(InvocationOnMock invocation) throws Throwable {
                final long currentQueueSize = queued.incrementAndGet();
                final GetRequest request = (GetRequest) invocation.getArguments()[0];
                final long current = counter.incrementAndGet();
                System.out.println(currentQueueSize + " queued at request #" + current + " (" + request.key() + ")");

                final Subject<CouchbaseResponse, CouchbaseResponse> observable = request.observable();

                if (currentQueueSize >= MAX_CAPACITY) {
                    if (overflowed.compareAndSet(false, true)) {
                        observable.onError(new BackpressureException());
                    }
                } else {
                    executor.schedule(new Runnable() {
                        @Override
                        public void run() {
                            if (overflowed.get())
                                return;

                            ByteBuf content = Unpooled.copiedBuffer(request.keyBytes());
                            GetResponse response = new GetResponse(ResponseStatus.SUCCESS, KeyValueStatus.SUCCESS.code(),
                                    0L, TranscoderUtils.STRING_COMMON_FLAGS, "test", content, request);
                            queued.decrementAndGet();
                            request.observable().onNext(response);
                            request.observable().onCompleted();
                        }
                    }, LATENCY, TimeUnit.MILLISECONDS);
                }

                return request.observable();
            }
        });
        return core;
    }

}
