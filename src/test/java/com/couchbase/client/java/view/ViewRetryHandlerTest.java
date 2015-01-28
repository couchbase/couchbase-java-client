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
package com.couchbase.client.java.view;

import com.couchbase.client.core.message.view.ViewQueryResponse;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
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
                when(response.info()).thenReturn(Observable.just(Unpooled.copiedBuffer("{\"err\": true}",
                    CharsetUtil.UTF_8)));
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
                when(response.info()).thenReturn(Observable.just(Unpooled.copiedBuffer("\n" +
                        "{\"error\":\"not_found\",\"reason\":\"Error opening view `al1l`, from set `default`, "
                        + "design document `_design/users`: {not_found,\\nmissing_named_view}\"}\n",
                    CharsetUtil.UTF_8)));
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