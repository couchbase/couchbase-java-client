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

package com.couchbase.client.java.util.retry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.couchbase.client.core.lang.Tuple;
import com.couchbase.client.core.time.Delay;
import com.couchbase.client.java.error.CannotRetryException;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.TestScheduler;

public class RetryWithDelayHandlerTest {

    private static final int MAX_ATTEMPTS = 10;

    private static RetryWithDelayHandler handler;

    @BeforeClass
    public static void setUp() throws Exception {
        handler = new RetryWithDelayHandler(MAX_ATTEMPTS, Delay.linear(TimeUnit.MILLISECONDS), new Func1<Throwable, Boolean>() {
            @Override
            public Boolean call(Throwable throwable) {
                return throwable instanceof OutOfMemoryError;
            }
        });
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
                testScheduler);

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
}