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
package com.couchbase.client.java.view;

import com.couchbase.client.core.message.view.ViewQueryResponse;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import org.junit.Test;
import rx.Observable;
import rx.Subscriber;
import rx.observers.TestSubscriber;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the functionality of the {@link ViewRetryHandler}.
 *
 * @author Michael Nitschinger
 * @since 2.0.2
 */
public class ViewRetryHandlerTest {

    @Test
    public void shouldNotRetrySuccess() {
        TestSubscriber<ViewQueryResponse> subscriber = new TestSubscriber<ViewQueryResponse>();

        final AtomicInteger subscriberCount = new AtomicInteger();
        Observable<ViewQueryResponse> observable = Observable.create(new Observable.OnSubscribe<ViewQueryResponse>() {
            @Override
            public void call(Subscriber<? super ViewQueryResponse> subscriber) {
                subscriberCount.incrementAndGet();
                ViewQueryResponse response = mock(ViewQueryResponse.class);
                when(response.info()).thenReturn(Observable.just(Unpooled.buffer()));
                when(response.responseCode()).thenReturn(200);
                subscriber.onNext(response);
                subscriber.onCompleted();
            }
        });

        ViewRetryHandler
            .retryOnCondition(observable)
            .subscribe(subscriber);

        subscriber.awaitTerminalEvent(1, TimeUnit.SECONDS);
        subscriber.assertNoErrors();
        assertEquals(1, subscriber.getOnNextEvents().size());
        assertEquals(1, subscriberCount.get());
    }

    @Test
    public void shouldRetryOnStatusCodes() {
        TestSubscriber<ViewQueryResponse> subscriber = new TestSubscriber<ViewQueryResponse>();

        final AtomicInteger subscriberCount = new AtomicInteger();
        Observable<ViewQueryResponse> observable = Observable.create(new Observable.OnSubscribe<ViewQueryResponse>() {
            @Override
            public void call(Subscriber<? super ViewQueryResponse> subscriber) {
                subscriberCount.incrementAndGet();
                ViewQueryResponse response = mock(ViewQueryResponse.class);
                int statusCode = subscriberCount.get() == 5 ? 200 : 300;
                when(response.responseCode()).thenReturn(statusCode);
                when(response.error()).thenReturn(Observable.just("{\"err\": true}"));
                subscriber.onNext(response);
                subscriber.onCompleted();
            }
        });

        ViewRetryHandler
            .retryOnCondition(observable)
            .subscribe(subscriber);

        subscriber.awaitTerminalEvent(1, TimeUnit.SECONDS);
        subscriber.assertNoErrors();
        assertEquals(1, subscriber.getOnNextEvents().size());
        assertEquals(5, subscriberCount.get());
    }

    @Test
    public void shouldPassThroughExceptions() {
        TestSubscriber<ViewQueryResponse> subscriber = new TestSubscriber<ViewQueryResponse>();

        final AtomicInteger subscriberCount = new AtomicInteger();
        Observable<ViewQueryResponse> observable = Observable.create(new Observable.OnSubscribe<ViewQueryResponse>() {
            @Override
            public void call(Subscriber<? super ViewQueryResponse> subscriber) {
                subscriberCount.incrementAndGet();
                subscriber.onError(new IllegalStateException());
            }
        });

        ViewRetryHandler
            .retryOnCondition(observable)
            .subscribe(subscriber);

        subscriber.awaitTerminalEvent(1, TimeUnit.SECONDS);
        assertEquals(1, subscriber.getOnErrorEvents().size());
        assertTrue(subscriber.getOnErrorEvents().get(0) instanceof IllegalStateException);
        assertEquals(1, subscriberCount.get());
    }

    @Test
    public void shouldNotRetryDesignDocNotFound() {
        TestSubscriber<ViewQueryResponse> subscriber = new TestSubscriber<ViewQueryResponse>();

        final AtomicInteger subscriberCount = new AtomicInteger();
        Observable<ViewQueryResponse> observable = Observable.create(new Observable.OnSubscribe<ViewQueryResponse>() {
            @Override
            public void call(Subscriber<? super ViewQueryResponse> subscriber) {
                subscriberCount.incrementAndGet();
                ViewQueryResponse response = mock(ViewQueryResponse.class);
                when(response.responseCode()).thenReturn(404);
                when(response.error()).thenReturn(Observable.just("\n" +
                        "{\"errors:\"[{\"error\":\"not_found\",\"reason\":\"Error opening view `al1l`, from set `default`, "
                        + "design document `_design/users`: {not_found,\\nmissing_named_view}\"}]}\n"));
                subscriber.onNext(response);
                subscriber.onCompleted();
            }
        });

        ViewRetryHandler
            .retryOnCondition(observable)
            .subscribe(subscriber);

        subscriber.awaitTerminalEvent(1, TimeUnit.SECONDS);
        subscriber.assertNoErrors();
        assertEquals(404, subscriber.getOnNextEvents().get(0).responseCode());
        assertEquals(1, subscriberCount.get());
        assertEquals(1, subscriber.getOnNextEvents().size());
    }
}