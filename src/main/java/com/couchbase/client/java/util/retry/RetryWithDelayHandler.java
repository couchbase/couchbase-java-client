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

import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.time.Delay;
import com.couchbase.client.core.time.ExponentialDelay;
import com.couchbase.client.java.error.CannotRetryException;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

/**
 * A class that allows to produce a "retry" delay depending on the number of retry attempts.
 * The number of retries is bounded by a maximum number of attempts.
 *
 * @see Retry#wrapForRetry(Observable, RetryWithDelayHandler) how to wrap an Observable with this behavior
 * @see RetryWhenFunction how to chain this behavior into an Observable's retryWhen operation.
 * @see RetryBuilder how to construct a RetryWhenFunction in a fluent manner.
 *
 * @author Simon Baslé
 * @since 2.1
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class RetryWithDelayHandler implements Func1<Tuple2<Integer, Throwable>, Observable<?>> {

    protected final int maxAttempts;
    protected final Delay retryDelay;
    protected final Func1<Throwable, Boolean> stoppingErrorFilter;
    protected final Scheduler optionalScheduler;

    /**
     * Construct a {@link RetryWithDelayHandler retry handler} that will retry on all errors.
     *
     * @param maxAttempts the maximum number of retries before a {@link CannotRetryException} is thrown.
     * @param retryDelay the {@link Delay} to apply between each retry (can grow,
     *  eg. by using {@link ExponentialDelay}).
     */
    public RetryWithDelayHandler(int maxAttempts, Delay retryDelay) {
        this(maxAttempts, retryDelay, null);
    }

    /**
     * Construct a {@link RetryWithDelayHandler retry handler} that will retry on most errors but will stop on specific errors.
     *
     * @param maxAttempts the maximum number of retries before a {@link CannotRetryException} is thrown.
     * @param retryDelay the {@link Delay} to apply between each retry (can grow,
     *  eg. by using {@link ExponentialDelay}).
     * @param stoppingErrorFilter a predicate that determine if an error must stop the retry cycle (when true),
     *  in which case said error is cascaded down.
     */
    public RetryWithDelayHandler(int maxAttempts, Delay retryDelay, Func1<Throwable, Boolean> stoppingErrorFilter) {
        this(maxAttempts, retryDelay, stoppingErrorFilter, null);
    }

    /**
     * Protected constructor that also allows to set a {@link Scheduler} for the delay, especially useful for tests.
     */
    public RetryWithDelayHandler(int maxAttempts, Delay retryDelay, Func1<Throwable, Boolean> stoppingErrorFilter,
        Scheduler scheduler) {
        this.maxAttempts = maxAttempts;
        this.retryDelay = retryDelay;
        this.stoppingErrorFilter = stoppingErrorFilter;
        this.optionalScheduler = scheduler;
    }

    protected static String messageForMaxAttempts(long reachedAfterNRetries) {
        return "maximum number of attempts reached after " + reachedAfterNRetries + " retries";
    }

    @Override
    public Observable<?> call(Tuple2<Integer, Throwable> attemptError) {
        long errorNumber = attemptError.value1();
        Throwable error = attemptError.value2();

        if (errorNumber > maxAttempts) {
            return Observable.error(new CannotRetryException(messageForMaxAttempts(errorNumber - 1), error));
        } else if (stoppingErrorFilter != null && stoppingErrorFilter.call(error) == Boolean.TRUE) {
            return Observable.error(error);
        } else {
            long delay = retryDelay.calculate(errorNumber);
            TimeUnit unit = retryDelay.unit();
            if (this.optionalScheduler != null) {
                return Observable.timer(delay, unit, optionalScheduler);
            } else {
                return Observable.timer(delay, unit);
            }
        }
    }
}
