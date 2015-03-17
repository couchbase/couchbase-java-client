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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.time.Delay;
import com.couchbase.client.java.error.CannotRetryException;
import org.junit.Test;

public class RetryBuilderTest {

    @Test
    public void testDefaultAttemptIsOne() {
        RetryWhenFunction result = RetryBuilder.any().build();

        assertEquals(1, result.handler.maxAttempts);
        assertSame(Retry.DEFAULT_DELAY, result.handler.retryDelay);
        assertNull(result.handler.stoppingErrorFilter);
    }

    @Test
    public void testRetryOnce() throws Exception {
        RetryWhenFunction result = RetryBuilder.any().once().build();

        assertEquals(1, result.handler.maxAttempts);
        assertSame(Retry.DEFAULT_DELAY, result.handler.retryDelay);
        assertNull(result.handler.stoppingErrorFilter);
    }

    @Test
    public void testRetryMax() throws Exception {
        RetryWhenFunction result = RetryBuilder.any().max(10).build();

        assertEquals(10, result.handler.maxAttempts);
        assertSame(Retry.DEFAULT_DELAY, result.handler.retryDelay);
        assertNull(result.handler.stoppingErrorFilter);
    }

    @Test
    public void testEmptyErrorsListMakesNullStoppingErrorFilter() {
        RetryWhenFunction neverSet = RetryBuilder.any().build();
        RetryWhenFunction emptyInclusion = RetryBuilder.anyOf().build();
        RetryWhenFunction emptyExclusion = RetryBuilder.allBut().build();

        assertEquals(null, neverSet.handler.stoppingErrorFilter);
        assertEquals(null, emptyInclusion.handler.stoppingErrorFilter);
        assertEquals(null, emptyExclusion.handler.stoppingErrorFilter);
    }

    @Test
    public void testOnlyWhenNot() throws Exception {
        RetryWhenFunction exclusive = RetryBuilder.allBut(CannotRetryException.class).build();

        assertTrue(exclusive.handler.stoppingErrorFilter instanceof RetryBuilder.ShouldStopOnError);
        assertTrue(exclusive.handler.stoppingErrorFilter.call(new CannotRetryException("")));
        assertFalse(exclusive.handler.stoppingErrorFilter.call(new IllegalStateException()));
    }

    @Test
    public void testOnlyWhen() throws Exception {
        RetryWhenFunction inclusive = RetryBuilder.anyOf(CannotRetryException.class).build();

        assertTrue(inclusive.handler.stoppingErrorFilter instanceof RetryBuilder.ShouldStopOnError);
        assertFalse(inclusive.handler.stoppingErrorFilter.call(new CannotRetryException("")));
        assertTrue(inclusive.handler.stoppingErrorFilter.call(new IllegalStateException()));
    }

    @Test
    public void testWithDelay() throws Exception {
        Delay linear = Delay.linear(TimeUnit.MINUTES, 8, 2, 2);
        RetryWhenFunction result = RetryBuilder.any().delay(linear).build();

        assertNotSame(Retry.DEFAULT_DELAY, result.handler.retryDelay);
        assertEquals(linear, result.handler.retryDelay);
    }
}