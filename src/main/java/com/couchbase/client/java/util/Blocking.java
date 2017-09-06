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
package com.couchbase.client.java.util;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.observables.BlockingObservable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Contains various utility methods related to blocking operations.
 *
 * @deprecated this class has been moved into core ({@link com.couchbase.client.core.utils.Blocking}
 * @author Michael Nitschinger
 * @since 2.0.2
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Private
@Deprecated
public class Blocking {

    private Blocking() {}

    /**
     * Blocks on an {@link Observable} and returns a single event or throws an {@link Exception}.
     *
     * Note that when this method is used, only the first item emitted will be returned. The caller needs to make
     * sure that the source {@link Observable} only ever returns a single item (or none). The {@link BlockingObservable}
     * code applies different operators like single, last, first and more, these need to be applied manually.
     *
     * This code is based on {@link BlockingObservable#blockForSingle}, but does not wait forever. Instead, it
     * utilizes the internal {@link CountDownLatch} to optimize the timeout case, with less GC and CPU overhead
     * than chaining in an {@link Observable#timeout(long, TimeUnit)} operator.
     *
     * If an error happens inside the {@link Observable}, it will be raised as an {@link Exception}. If the timeout
     * kicks in, a {@link TimeoutException} nested in a {@link RuntimeException} is thrown to be fully compatible
     * with the {@link Observable#timeout(long, TimeUnit)} behavior.
     *
     * @param observable the source {@link Observable}
     * @param timeout the maximum timeout before an exception is thrown.
     * @param tu the timeout unit.
     * @param <T> the type returned.
     * @return the extracted value from the {@link Observable} or throws in an error case.
     */
    public static <T> T blockForSingle(final Observable<? extends T> observable, final long timeout,
        final TimeUnit tu) {
        final CountDownLatch latch = new CountDownLatch(1);
        TrackingSubscriber<T> subscriber = new TrackingSubscriber<T>(latch);

        Subscription subscription = observable.subscribe(subscriber);

        try {
            if (!latch.await(timeout, tu)) {
                if (!subscription.isUnsubscribed()) {
                    subscription.unsubscribe();
                }
                throw new RuntimeException(new TimeoutException());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for subscription to complete.", e);
        }

        if (subscriber.returnException() != null) {
            if (subscriber.returnException() instanceof RuntimeException) {
                throw (RuntimeException) subscriber.returnException();
            } else {
                throw new RuntimeException(subscriber.returnException());
            }
        }

        return subscriber.returnItem();
    }


    /**
     * A {@link Subscriber} which tracks the returned item or exception.
     *
     * By pushing out the {@link Subscriber} in it's own class, the code
     * can get rid of {@link AtomicReference} objects and stick with
     * plain volatiles instead (since it just needs get/set semantics) and
     * reduce allocations a little bit.
     *
     * @since 2.2.0
     */
    private final static class TrackingSubscriber<T> extends Subscriber<T> {

        private final CountDownLatch latch;
        private volatile T returnItem = null;
        private volatile Throwable returnException = null;

        public TrackingSubscriber(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onCompleted() {
            latch.countDown();
        }

        @Override
        public void onError(final Throwable e) {
            returnException = e;
            latch.countDown();
        }

        @Override
        public void onNext(final T item) {
            returnItem = item;
        }

        public Throwable returnException() {
            return returnException;
        }

        public T returnItem() {
            return returnItem;
        }
    }

}
