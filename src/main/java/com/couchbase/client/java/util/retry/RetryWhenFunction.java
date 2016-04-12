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
@InterfaceStability.Committed
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
