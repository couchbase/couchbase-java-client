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