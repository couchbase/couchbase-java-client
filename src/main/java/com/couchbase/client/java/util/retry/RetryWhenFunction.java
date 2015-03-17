/**
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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import rx.Observable;
import rx.functions.Func1;

/**
 * Combine a {@link Retry#errorsWithAttempts(Observable, int) mapping of errors to their attempt number} with
 * a flatmap that {@link RetryWithDelayHandler induces a retry delay} into a function that can be passed to
 * an Observable's {@link Observable#retryWhen(Func1) retryWhen operation}.
 *
 * @see RetryBuilder how to construct such a function in a fluent manner.
 *
 * @author Simon Baslé
 * @since 2.1
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class RetryWhenFunction implements Func1<Observable<? extends Throwable>, Observable<?>> {

    protected RetryWithDelayHandler handler;

    public RetryWhenFunction(RetryWithDelayHandler handler) {
        this.handler = handler;
    }

    public Observable<?> call(Observable<? extends Throwable> errors) {
        return Retry.errorsWithAttempts(errors, handler.maxAttempts + 1)
                    .flatMap(handler);
    }
}
