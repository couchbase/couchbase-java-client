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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.couchbase.client.core.time.Delay;
import com.couchbase.client.java.error.CannotRetryException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;

public class RetryBuilderTest {

    @Test
    public void testDefaultAttemptIsOne() {
        RetryWhenFunction result = RetryBuilder.any().build();

        assertEquals(1, result.handler.maxAttempts);
        assertSame(Retry.DEFAULT_DELAY, result.handler.retryDelay);
        assertNull(result.handler.errorInterruptingPredicate);
    }

    @Test
    public void testRetryOnce() throws Exception {
        RetryWhenFunction result = RetryBuilder.any().once().build();

        assertEquals(1, result.handler.maxAttempts);
        assertSame(Retry.DEFAULT_DELAY, result.handler.retryDelay);
        assertNull(result.handler.errorInterruptingPredicate);
    }

    @Test
    public void testRetryMax() throws Exception {
        RetryWhenFunction result = RetryBuilder.any().max(10).build();

        assertEquals(10, result.handler.maxAttempts);
        assertSame(Retry.DEFAULT_DELAY, result.handler.retryDelay);
        assertNull(result.handler.errorInterruptingPredicate);
    }

    @Test
    public void testRetryMaxIsCappedAtIntegerMaxValueMinusOne() {
        RetryWhenFunction result = RetryBuilder.any().max(Integer.MAX_VALUE).build();

        assertEquals(Integer.MAX_VALUE - 1, result.handler.maxAttempts);
    }

    @Test
    public void testEmptyErrorsListMakesNullStoppingErrorFilter() {
        RetryWhenFunction neverSet = RetryBuilder.any().build();
        RetryWhenFunction emptyInclusion = RetryBuilder.anyOf().build();
        RetryWhenFunction emptyExclusion = RetryBuilder.allBut().build();

        assertEquals(null, neverSet.handler.errorInterruptingPredicate);
        assertEquals(null, emptyInclusion.handler.errorInterruptingPredicate);
        assertEquals(null, emptyExclusion.handler.errorInterruptingPredicate);
    }

    @Test
    public void testOnlyWhenNot() throws Exception {
        RetryWhenFunction exclusive = RetryBuilder.allBut(CannotRetryException.class).build();

        assertTrue(exclusive.handler.errorInterruptingPredicate instanceof RetryBuilder.ShouldStopOnError);
        assertTrue(exclusive.handler.errorInterruptingPredicate.call(new CannotRetryException("")));
        assertFalse(exclusive.handler.errorInterruptingPredicate.call(new IllegalStateException()));
    }

    @Test
    public void testOnlyWhen() throws Exception {
        RetryWhenFunction inclusive = RetryBuilder.anyOf(CannotRetryException.class).build();

        assertTrue(inclusive.handler.errorInterruptingPredicate instanceof RetryBuilder.ShouldStopOnError);
        assertFalse(inclusive.handler.errorInterruptingPredicate.call(new CannotRetryException("")));
        assertTrue(inclusive.handler.errorInterruptingPredicate.call(new IllegalStateException()));
    }

    @Test
    public void testWithDelay() throws Exception {
        Delay linear = Delay.linear(TimeUnit.MINUTES, 8, 2, 2);
        RetryWhenFunction result = RetryBuilder.any().delay(linear).build();

        assertNotSame(Retry.DEFAULT_DELAY, result.handler.retryDelay);
        assertEquals(linear, result.handler.retryDelay);
    }

    @Test
    public void testErrorPredicateIsInverted() {
        Func1<Throwable, Boolean> errorRetriesPredicate = new Func1<Throwable, Boolean>() {
            @Override
            public Boolean call(Throwable throwable) {
                return "toto".equals(throwable.getMessage());
            }
        };
        RetryWhenFunction fun = RetryBuilder.anyMatches(errorRetriesPredicate).build();

        RuntimeException retryException = new RuntimeException("toto");
        RuntimeException stopException = new RuntimeException("abc");

        assertTrue(errorRetriesPredicate.call(retryException));
        assertFalse(errorRetriesPredicate.call(stopException));

        assertTrue(fun.handler.errorInterruptingPredicate instanceof RetryBuilder.InversePredicate);
        assertNotEquals(errorRetriesPredicate.call(retryException), fun.handler.errorInterruptingPredicate.call(retryException));
        assertNotEquals(errorRetriesPredicate.call(stopException), fun.handler.errorInterruptingPredicate.call(stopException));
    }

    @Test(expected = DocumentDoesNotExistException.class)
    public void shouldNotIgnoreExceptionOnLastTry() {
        final AtomicInteger attempt = new AtomicInteger(0);
        Observable
            .just(0)
            .flatMap(new Func1<Integer, Observable<Integer>>() {
                @Override
                public Observable<Integer> call(Integer l) {
                    int at = attempt.getAndIncrement();
                    if (at == 0 || at == 1) {
                        return Observable.error(new TimeoutException());
                    } else {
                        return Observable.error(new DocumentDoesNotExistException());
                    }
                }
            })
            .retryWhen(RetryBuilder.anyOf(TimeoutException.class).max(2).build())
            .toBlocking()
            .single();
    }
}