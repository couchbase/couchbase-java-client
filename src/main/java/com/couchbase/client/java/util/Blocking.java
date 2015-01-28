/**
 * Copyright (C) 2014 Couchbase, Inc.
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
package com.couchbase.client.java.util;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import rx.Observable;
import rx.Subscriber;
import rx.observables.BlockingObservable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Contains various utility methods related to blocking operations.
 *
 * @author Michael Nitschinger
 * @since 2.0.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Private
public class Blocking {

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
        final AtomicReference<T> returnItem = new AtomicReference<T>();
        final AtomicReference<Throwable> returnException = new AtomicReference<Throwable>();
        final CountDownLatch latch = new CountDownLatch(1);

        observable
            .subscribe(new Subscriber<T>() {
                @Override
                public void onCompleted() {
                    latch.countDown();
                }

                @Override
                public void onError(final Throwable e) {
                    returnException.set(e);
                    latch.countDown();
                }

                @Override
                public void onNext(final T item) {
                    returnItem.set(item);
                }
            });

        try {
            if (!latch.await(timeout, tu)) {
                throw new RuntimeException(new TimeoutException());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for subscription to complete.", e);
        }

        if (returnException.get() != null) {
            if (returnException.get() instanceof RuntimeException) {
                throw (RuntimeException) returnException.get();
            } else {
                throw new RuntimeException(returnException.get());
            }
        }

        return returnItem.get();
    }

}
