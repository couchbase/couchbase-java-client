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

import static org.junit.Assert.*;

import java.util.List;

import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.java.error.CannotRetryException;
import org.junit.Test;
import rx.Observable;

public class ErrorsAttemptsTest {

    @Test
    public void testErrorsWithAttempts() throws Exception {
        Observable<Throwable> errors = Observable.<Throwable>just(new CannotRetryException("")).repeat(10);
        Observable<Tuple2<Integer, Throwable>> errorsWithAttempts = Retry.errorsWithAttempts(
                errors, 10);

        List<Tuple2<Integer, Throwable>> list = errorsWithAttempts.toList().toBlocking().first();

        assertEquals(10, list.size());
        int expectedNumber = 1;
        for (Tuple2<Integer, Throwable> tuple2 : list) {
            assertEquals(expectedNumber++, tuple2.value1().intValue());
        }
    }
    @Test
    public void testErrorsWithAttemptsIsBoundedByErrors() throws Exception {
        Observable<Throwable> errors = Observable.<Throwable>just(new CannotRetryException("")).repeat(10);
        Observable<Tuple2<Integer, Throwable>> errorsWithAttempts = Retry.errorsWithAttempts(
                errors, 100);

        List<Tuple2<Integer, Throwable>> list = errorsWithAttempts.toList().toBlocking().first();

        assertEquals(10, list.size());
    }
    @Test
    public void testErrorsWithAttemptsIsBoundedByMaxAttempts() throws Exception {
        Observable<Throwable> errors = Observable.<Throwable>just(new CannotRetryException("")).repeat(100);
        Observable<Tuple2<Integer, Throwable>> errorsWithAttempts = Retry.errorsWithAttempts(
                errors, 10);

        List<Tuple2<Integer, Throwable>> list = errorsWithAttempts.toList().toBlocking().first();

        assertEquals(10, list.size());
    }
}