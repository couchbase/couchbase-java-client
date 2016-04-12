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

package com.couchbase.client.java.util.retry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.couchbase.client.core.lang.Tuple;
import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.time.Delay;
import com.couchbase.client.java.error.CannotRetryException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.functions.Func1;
import rx.observers.TestSubscriber;
import rx.schedulers.TestScheduler;

public class RetryWithDelayHandlerTest {

    private static final int MAX_ATTEMPTS = 10;

    private static RetryWithDelayHandler handler;

    @BeforeClass
    public static void setUp() throws Exception {
        handler = new RetryWithDelayHandler(MAX_ATTEMPTS, Delay.linear(TimeUnit.MILLISECONDS),
                new Func1<Throwable, Boolean>() {
                    @Override
                    public Boolean call(Throwable throwable) {
                        return throwable instanceof OutOfMemoryError;
                    }
                }, null);
    }

    @Test
    public void shouldThrowWhenMaxAttemptReached() {
        try {
            handler.call(Tuple.create(MAX_ATTEMPTS + 1, (Throwable) new IllegalStateException())).toBlocking().last();
            fail("expected to throw");
        } catch (CannotRetryException e) {
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof IllegalStateException);
            assertEquals(e.getMessage(), RetryWithDelayHandler.messageForMaxAttempts(MAX_ATTEMPTS), e.getMessage());
        }
    }

    @Test(expected = OutOfMemoryError.class)
    public void shouldThrowWhenErrorBlockingRetry() {
        handler.call(Tuple.create(MAX_ATTEMPTS - 2, (Throwable) new OutOfMemoryError())).toBlocking().last();
        fail("expected to throw");
    }

    @Test
    public void shouldDelayLinearlyWhenErrorCanBeRetried() {
        TestScheduler testScheduler = new TestScheduler();

        RetryWithDelayHandler timeHandler = new RetryWithDelayHandler(
                MAX_ATTEMPTS,
                Delay.linear(TimeUnit.SECONDS),
                new Func1<Throwable, Boolean>() {
                    @Override
                    public Boolean call(Throwable throwable) {
                        return throwable instanceof OutOfMemoryError;
                    }
                },
                null, testScheduler);

        final AtomicLong atomicLong = new AtomicLong(-1L);
        timeHandler.call(Tuple.create(MAX_ATTEMPTS - 2, (Throwable) new IllegalStateException()))
            .subscribe(new Action1<Object>() {
                @Override
                public void call(Object o) {
                    if (o instanceof Long) {
                        atomicLong.compareAndSet(-1L, (Long) o);
                    }
                }
            });

        testScheduler.advanceTimeBy(7, TimeUnit.SECONDS);
        assertEquals(-1L, atomicLong.longValue());

        testScheduler.advanceTimeBy(8, TimeUnit.SECONDS);
        assertEquals(0L, atomicLong.longValue()); //0L is the value emitted by Observable.timer
    }

    @Test
    public void shouldLimitMaxAttemptsToIntegerMaxValueMinusOne() {
        int normal = 4000;
        int underLimit = Integer.MAX_VALUE - 1;
        int atLimit = Integer.MAX_VALUE;

        RetryWithDelayHandler testNormal = new RetryWithDelayHandler(normal, Retry.DEFAULT_DELAY, null, null);
        RetryWithDelayHandler testUnderLimit = new RetryWithDelayHandler(underLimit, Retry.DEFAULT_DELAY, null, null);
        RetryWithDelayHandler testAtLimit = new RetryWithDelayHandler(atLimit, Retry.DEFAULT_DELAY, null, null);

        assertEquals(normal, testNormal.maxAttempts);
        assertEquals(underLimit, testUnderLimit.maxAttempts);
        assertEquals(atLimit - 1, testAtLimit.maxAttempts);
    }

        @Test
    public void shouldCallDoOnRetryBeforeEachRetryUntilExceptionNotRetriable() {
        TestScheduler testScheduler = new TestScheduler();
        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();
        final LinkedList<String> retryLog = new LinkedList<String>();

        RetryWithDelayHandler timeHandler = new RetryWithDelayHandler(
                MAX_ATTEMPTS,
                Delay.linear(TimeUnit.SECONDS),
                new Func1<Throwable, Boolean>() {
                    @Override
                    public Boolean call(Throwable throwable) {
                        return throwable instanceof IllegalArgumentException;
                    }
                },
                new RetryBuilder.OnRetryAction() {
                    @Override
                    public void call(Integer attempt, Throwable retriedError, Long delay, TimeUnit timeUnit) {
                        String logLine = "Retry #" + attempt + " in " + delay + " " + timeUnit +
                            " for " + retriedError;
                        retryLog.add(logLine);
                    }
                }, testScheduler);

        final AtomicLong step = new AtomicLong(0);
        Observable<String> erroring = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (step.getAndIncrement() < 3)
                    subscriber.onError(new IllegalStateException());
                else
                    subscriber.onError(new IllegalArgumentException());
            }
        });
        erroring.retryWhen(new RetryWhenFunction(timeHandler), testScheduler).subscribe(testSubscriber);

        assertEquals(0, retryLog.size());
        testSubscriber.assertNoValues();
        testSubscriber.assertNoTerminalEvent();

        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);
        assertEquals(1, retryLog.size());
        assertEquals("Retry #1 in 1 SECONDS for java.lang.IllegalStateException", retryLog.getLast());
        testSubscriber.assertNoValues();
        testSubscriber.assertNoTerminalEvent();

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        assertEquals(2, retryLog.size());
        assertEquals("Retry #2 in 2 SECONDS for java.lang.IllegalStateException", retryLog.getLast());
        testSubscriber.assertNoValues();
        testSubscriber.assertNoTerminalEvent();

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        assertEquals(3, retryLog.size());
        assertEquals("Retry #3 in 3 SECONDS for java.lang.IllegalStateException", retryLog.getLast());
        testSubscriber.assertNoValues();
        testSubscriber.assertNoTerminalEvent();

        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS);
        assertEquals(3, retryLog.size());
        testSubscriber.assertNoValues();
        testSubscriber.assertError(IllegalArgumentException.class);
    }

    @Test
    public void shouldCallDoOnRetryBeforeEachRetryUntilMaxAttempt() {
        TestScheduler testScheduler = new TestScheduler();
        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();
        final LinkedList<String> retryLog = new LinkedList<String>();

        RetryWithDelayHandler timeHandler = new RetryWithDelayHandler(
                3,
                Delay.linear(TimeUnit.SECONDS),
                new Func1<Throwable, Boolean>() {
                    @Override
                    public Boolean call(Throwable throwable) {
                        return throwable instanceof OutOfMemoryError;
                    }
                },
                new RetryBuilder.OnRetryAction() {
                    @Override
                    public void call(Integer attempt, Throwable retriedError, Long delay, TimeUnit timeUnit) {
                        String logLine = "Retry #" + attempt + " in " + delay + " " + timeUnit +
                            " for " + retriedError;
                        retryLog.add(logLine);
                    }
                }, testScheduler);

        Observable<String> erroring = Observable.error(new IllegalStateException());
        erroring.retryWhen(new RetryWhenFunction(timeHandler), testScheduler)
                .doOnError(new Action1<Throwable>() {
                               @Override
                               public void call(Throwable throwable) {
                                   System.out.println(throwable);
                               }
                           }
                )
                .subscribe(testSubscriber);

        assertEquals(0, retryLog.size());
        testSubscriber.assertNoValues();
        testSubscriber.assertNoTerminalEvent();

        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);
        assertEquals(1, retryLog.size());
        assertEquals("Retry #1 in 1 SECONDS for java.lang.IllegalStateException", retryLog.getLast());
        testSubscriber.assertNoValues();
        testSubscriber.assertNoTerminalEvent();

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        assertEquals(2, retryLog.size());
        assertEquals("Retry #2 in 2 SECONDS for java.lang.IllegalStateException", retryLog.getLast());
        testSubscriber.assertNoValues();
        testSubscriber.assertNoTerminalEvent();

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        assertEquals(3, retryLog.size());
        assertEquals("Retry #3 in 3 SECONDS for java.lang.IllegalStateException", retryLog.getLast());
        testSubscriber.assertNoValues();
        testSubscriber.assertNoTerminalEvent();

        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS);
        assertEquals(3, retryLog.size());
        testSubscriber.assertNoValues();
        testSubscriber.assertError(CannotRetryException.class);
    }
}