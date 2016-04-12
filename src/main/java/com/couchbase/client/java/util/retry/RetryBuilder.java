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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.time.Delay;
import com.couchbase.client.java.error.CannotRetryException;
import rx.Scheduler;
import rx.functions.Action4;
import rx.functions.Func1;

/**
 * Builder for {@link RetryWhenFunction}. Start with {@link #any()}, {@link #anyOf(Class[])} or {@link #allBut(Class[])}
 * factory methods.
 *
 * By default, without calling additional methods it will retry on the specified exceptions, with a constant delay
 * (see {@link Retry#DEFAULT_DELAY}), and only once.
 *
 * Note that if retriable errors keep occurring more than the maximum allowed number of attempts, the last error that
 * triggered the extraneous attempt will be wrapped as the cause inside a {@link CannotRetryException}, which will be
 * emitted via the observable's onError method.
 *
 * @author Simon Baslé
 * @since 2.1
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public class RetryBuilder {

    private int maxAttempts;

    private Delay delay;

    private List<Class<? extends Throwable>> errorsStoppingRetry;
    private Action4<Integer, Throwable, Long, TimeUnit> doOnRetryAction;
    private Func1<Throwable, Boolean> retryErrorPredicate;

    private boolean inverse;

    private Scheduler scheduler;


    private RetryBuilder() {
        this.maxAttempts = 1; //one attempt
        this.delay = Retry.DEFAULT_DELAY; //constant 1ms
        this.errorsStoppingRetry = null; //retry on any error
        this.inverse = false; //list above is indeed list of errors that can stop retry (none)
        this.scheduler = null; //operate on default Scheduler for timer delay
        this.doOnRetryAction = null; //no retry side effect
        this.retryErrorPredicate = null; //retry purely on the instanceOf
    }

    /** Only errors that are instanceOf the specified types will trigger a retry */
    public static RetryBuilder anyOf(Class<? extends Throwable>... types) {
        RetryBuilder retryBuilder = new RetryBuilder();
        retryBuilder.maxAttempts = 1;

        retryBuilder.errorsStoppingRetry = Arrays.asList(types);
        retryBuilder.inverse = true;

        return retryBuilder;
    }

    /** Only errors that are NOT instanceOf the specified types will trigger a retry */
    public static RetryBuilder allBut(Class<? extends Throwable>... types) {
        RetryBuilder retryBuilder = new RetryBuilder();
        retryBuilder.maxAttempts = 1;

        retryBuilder.errorsStoppingRetry = Arrays.asList(types);
        retryBuilder.inverse = false;

        return retryBuilder;
    }

    /** Any error will trigger a retry */
    public static RetryBuilder any() {
        RetryBuilder retryBuilder = new RetryBuilder();
        retryBuilder.maxAttempts = 1;
        return retryBuilder;
    }

    /** Any error that pass the predicate will trigger a retry */
    public static RetryBuilder anyMatches(Func1<Throwable, Boolean> retryErrorPredicate) {
        RetryBuilder retryBuilder = new RetryBuilder();
        retryBuilder.maxAttempts = 1;

        retryBuilder.retryErrorPredicate = retryErrorPredicate;
        return retryBuilder;
    }

    /**
     * Make only one retry attempt (default).
     *
     * If an error that can trigger a retry occurs twice in a row, it will be wrapped as the cause inside a
     * {@link CannotRetryException}, which will be emitted via the observable's onError method.
     */
    public RetryBuilder once() {
        this.maxAttempts = 1;
        return this;
    }

    /**
     * Make at most maxAttempts retry attempts.
     *
     * Note that the maximum accepted value is <code>{@link Integer#MAX_VALUE}
     * - 1</code>, the internal retry mechanism will ensure a total of <code>maxAttempts + 1</code> total attempts,
     * accounting for the original call.
     *
     * If an error that can trigger a retry occurs more that <i>maxAttempts</i>, it will be wrapped as the
     * cause inside a {@link CannotRetryException}, which will be emitted via the observable's onError method.
     */
    public RetryBuilder max(int maxAttempts) {
        this.maxAttempts = Math.min(maxAttempts, Integer.MAX_VALUE - 1);
        return this;
    }

    /** Customize the retry {@link Delay} */
    public RetryBuilder delay(Delay delay) {
        return delay(delay, null);
    }

    /** Use {@link Retry#DEFAULT_DELAY} but wait on a specific {@link Scheduler} */
    public RetryBuilder delay(Scheduler scheduler) {
        return delay(null, scheduler);
    }

    /**
     * Set both the {@link Delay} and the {@link Scheduler} on which the delay is waited.
     * If the delay is null, {@link Retry#DEFAULT_DELAY} is used.
     */
    public RetryBuilder delay(Delay delay, Scheduler scheduler) {
        this.delay = (delay == null) ? Retry.DEFAULT_DELAY : delay;
        this.scheduler = scheduler;
        return this;
    }

    /**
     * Execute some code each time a retry is scheduled (at the moment the retriable exception
     * is caught, but before the retry delay is applied). Only quick executing code should be
     * performed, do not block in this action.
     *
     * The action receives the retry attempt number (1-n), the exception that caused the retry,
     * the delay duration and timeunit for the scheduled retry.
     *
     * @param doOnRetryAction the side-effect action to perform whenever a retry is scheduled.
     * @see OnRetryAction if you want a shorter signature.
     */
    public RetryBuilder doOnRetry(Action4<Integer, Throwable, Long, TimeUnit> doOnRetryAction) {
        this.doOnRetryAction = doOnRetryAction;
        return this;
    }

    /** Construct the resulting {@link RetryWhenFunction} */
    public RetryWhenFunction build() {
        RetryWithDelayHandler handler;
        Func1<Throwable, Boolean> filter;
        if ((errorsStoppingRetry == null || errorsStoppingRetry.isEmpty()) && retryErrorPredicate == null) {
            //always retry on any error
            filter = null;
        } else if (retryErrorPredicate != null) {
            filter = new InversePredicate(retryErrorPredicate);
        } else {
            filter = new ShouldStopOnError(errorsStoppingRetry, inverse);
        }

        if (scheduler == null) {
            handler = new RetryWithDelayHandler(maxAttempts, delay, filter, doOnRetryAction);
        } else {
            handler = new RetryWithDelayHandler(maxAttempts, delay, filter, doOnRetryAction, scheduler);
        }
        return new RetryWhenFunction(handler);
    }

    protected static class InversePredicate implements Func1<Throwable, Boolean> {

        private final Func1<Throwable, Boolean> predicate;

        public InversePredicate(final Func1<Throwable, Boolean> predicate) {
            this.predicate = predicate;
        }

        @Override
        public Boolean call(Throwable throwable) {
            Boolean toInvert = predicate.call(throwable);
            if (toInvert == null) {
                return null;
            } else if (Boolean.TRUE.equals(toInvert)) {
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        }
    }

    protected static class ShouldStopOnError implements Func1<Throwable, Boolean> {

        private final List<Class<? extends Throwable>> errorsStoppingRetry;
        private final boolean inverse;

        public ShouldStopOnError(List<Class<? extends Throwable>> filterErrorList, boolean inverse) {
            this.errorsStoppingRetry = filterErrorList;
            this.inverse = inverse;
        }

        @Override
        public Boolean call(Throwable o) {
            //if inverse is false, only errors in the list should prevent retry
            //if inverse is true, all errors except ones in list should prevent retry
            for (Class<? extends Throwable> aClass : errorsStoppingRetry) {
                if (aClass.isInstance(o)) {
                    return !inverse;
                }
            }
            return inverse;
        }
    }

    /**
     * An interface alias for <code>Action4&lt;Integer, Throwable, Long, TimeUnit&gt;</code>, suitable
     * for {@link RetryBuilder#doOnRetry(Action4)}.
     */
    public interface OnRetryAction extends Action4<Integer, Throwable, Long, TimeUnit> {

    }
}
