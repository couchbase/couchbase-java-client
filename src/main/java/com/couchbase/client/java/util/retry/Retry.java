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

import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.lang.Tuple;
import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.time.Delay;
import rx.Observable;
import rx.functions.Func2;

/**
 * Utility methods to deal with retrying {@link Observable}s.
 *
 * @author Simon Baslé
 * @since 2.1
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public class Retry {

    public static final Delay DEFAULT_DELAY = Delay.fixed(1, TimeUnit.MILLISECONDS);

    private Retry() {}

    /**
     * Wrap an {@link Observable} so that it will retry on all errors for a maximum number of times.
     * The retry is almost immediate (1ms delay).
     *
     * @param source the {@link Observable} to wrap.
     * @param maxAttempts the maximum number of times to attempt a retry. It will be capped at <code>{@link Integer#MAX_VALUE} - 1</code>.
     * @param <T> the type of items emitted by the source Observable.
     * @return the wrapped retrying Observable.
     */
    public static <T> Observable<T> wrapForRetry(Observable<T> source, int maxAttempts) {
        return wrapForRetry(source, new RetryWithDelayHandler(maxAttempts, DEFAULT_DELAY));
    }

    /**
     * Wrap an {@link Observable} so that it will retry on all errors. The retry will occur for a maximum number of
     * attempts and with a provided {@link Delay} between each attempt.
     *
     * @param source the {@link Observable} to wrap.
     * @param maxAttempts the maximum number of times to attempt a retry. It will be capped at <code>{@link Integer#MAX_VALUE} - 1</code>.
     * @param retryDelay the {@link Delay} between each attempt.
     * @param <T> the type of items emitted by the source Observable.
     * @return the wrapped retrying Observable.
     */
    public static <T> Observable<T> wrapForRetry(Observable<T> source, int maxAttempts, Delay retryDelay) {
        return wrapForRetry(source, new RetryWithDelayHandler(maxAttempts, retryDelay));
    }

    /**
     * Wrap an {@link Observable} so that it will retry on some errors. The retry will occur for a maximum number of
     * attempts and with a provided {@link Delay} between each attempt represented by the {@link RetryWithDelayHandler},
     * which can also filter on errors and stop the retry cycle for certain type of errors.
     *
     * @param source the {@link Observable} to wrap.
     * @param handler the {@link RetryWithDelayHandler}, describes maximum number of attempts, delay and fatal errors.
     * @param <T> the type of items emitted by the source Observable.
     * @return the wrapped retrying Observable.
     */
    public static <T> Observable<T> wrapForRetry(Observable<T> source, final RetryWithDelayHandler handler) {
        return source.retryWhen(new RetryWhenFunction(handler));
    }

    /**
     * Internal utility method to combine errors in an observable with their attempt number.
     *
     * @param errors the errors.
     * @param expectedAttempts the maximum of combinations to make (for retry, should be the maximum number of
     *                         authorized retries + 1).
     * @return an Observable that combines the index/attempt number of each error with its error in a {@link Tuple2}.
     */
    protected static Observable<Tuple2<Integer, Throwable>> errorsWithAttempts(Observable<? extends Throwable> errors,
            final int expectedAttempts) {
        return errors.zipWith(
                Observable.range(1, expectedAttempts),
                new Func2<Throwable, Integer, Tuple2<Integer, Throwable>>() {
                    @Override
                    public Tuple2<Integer, Throwable> call(Throwable error, Integer attempt) {
                        return Tuple.create(attempt, error);
                    }
                }
        );
    }

}
