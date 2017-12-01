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

import com.couchbase.client.deps.io.netty.util.ReferenceCounted;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.Exceptions;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observers.Subscribers;
import rx.subjects.AsyncSubject;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Defers the execution of a {@link rx.subjects.Subject} and in addition watches for early unsubscription and
 * cleans up buffers if the content is {@link ReferenceCounted}.
 *
 * Implementation Details:
 *
 * This implementation is very similar to {@link Observable#defer(Func0)} in that it takes a hot observable like
 * a subject and defers the execution of it until someone subscribes. The problem with vanilla defer is that if
 * an early unsubscribe happens (like a downstream timeout firing) the message from the hot observable is not
 * properly consumed anymore which can lead to buffer leaks if it contains a pooled resource.
 *
 * To mitigate this, another subscription is added to the hot observable which checks, at the time of item emission,
 * that the subscription is still present. If it is not the buffers are proactively cleared out, making sure that
 * no trace/leak is left behind.
 *
 *  ♬ Wir hom so vü zum tuan                                ♬ We have so much to do
 *  ♬ Wir hudln und schurdln ummanond                       ♬ We are hurrying and botching around
 *  ♬ Müssen uns sputen des dauert vü zu lang               ♬ We need to hurry, it all takes too long
 *  ♬ Da hüft ka hupen so kummst a ned schneller dran       ♬ No need to honk, you’re not getting served quicker
 *  ♬ Und a ka Fluchen lametier ned gemmas on               ♬ No swearing, no whining, lets get on with it
 *      -- from Skero - "Hudeln"
 *
 * @author Michael Nitschinger
 * @since 2.3.6
 */
public class OnSubscribeDeferAndWatch<T> implements Observable.OnSubscribe<T> {

    /**
     * Defer a hot observable and clean its buffers if needed on early unsubscribe. It currently only works if you
     * are deferring a {@link AsyncSubject}.
     *
     * @param observableFactory the factory of the hot observable.
     * @return a deferred observable which handles cleanup of resources on early unsubscribe.
     */
    public static <T> Observable<T> deferAndWatch(Func1<Subscriber,? extends Observable<? extends T>> observableFactory) {
        return Observable.create(new OnSubscribeDeferAndWatch<T>(observableFactory));
    }

    private final Func1<Subscriber,? extends Observable<? extends T>> observableFactory;

    private OnSubscribeDeferAndWatch(Func1<Subscriber,? extends Observable<? extends T>> observableFactory) {
        this.observableFactory = observableFactory;
    }

    @Override
    public void call(Subscriber<? super T> s) {

        // Defer execution of the hot observable.
        Observable<? extends T> o;
        try {
            o = observableFactory.call(s);
        } catch (Throwable t) {
            Exceptions.throwOrReport(t, s);
            return;
        }

        // Hook up the consumer subscription and store it in a reference
        final AtomicReference<Subscription> sr = new AtomicReference<Subscription>();
        final AtomicBoolean emitted = new AtomicBoolean(false);
        sr.set(o.doOnNext(new Action1<T>() {
            @Override
            public void call(T t) {
                emitted.set(true);
            }
        }).unsafeSubscribe(Subscribers.wrap(s)));

        // Add the additional subscription to the hot observable which once an item is emitted
        // will check if the original subscription is still present and if not it will release
        // the buffer.
        o.subscribe(new Subscriber<T>() {
            @Override
            public void onCompleted() {
                // ignored on purpose
            }

            @Override
            public void onError(Throwable e) {
                // ignored on purpose
            }

            @Override
            public void onNext(T t) {
                if (t != null && !emitted.get() && sr.get().isUnsubscribed() && t instanceof ReferenceCounted) {
                    ReferenceCounted rc = (ReferenceCounted) t;
                    if (rc.refCnt() > 0) {
                        rc.release();
                    }
                }
            }
        });
    }
}
