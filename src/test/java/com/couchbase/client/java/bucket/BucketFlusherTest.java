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
package com.couchbase.client.java.bucket;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.endpoint.kv.KeyValueStatus;
import com.couchbase.client.core.message.CouchbaseResponse;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.config.FlushRequest;
import com.couchbase.client.core.message.config.FlushResponse;
import com.couchbase.client.core.message.kv.GetRequest;
import com.couchbase.client.core.message.kv.GetResponse;
import com.couchbase.client.core.message.kv.UpsertRequest;
import com.couchbase.client.core.message.kv.UpsertResponse;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.java.error.FlushDisabledException;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import rx.Observable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the functionality of the {@link BucketFlusher}.
 *
 * @author Michael Nitschinger
 * @since 2.1.1
 */
public class BucketFlusherTest {

    private static final String BUCKET = "default";
    private static final String PASSWORD = "";

    private static final CouchbaseResponse GOOD_FLUSH_RESPONSE = new FlushResponse(true, "", ResponseStatus.SUCCESS);
    private static final CouchbaseResponse PEND_FLUSH_RESPONSE = new FlushResponse(false, "", ResponseStatus.SUCCESS);
    private static final CouchbaseResponse GOOD_UPSERT_RESPONSE = new UpsertResponse(ResponseStatus.SUCCESS,
            KeyValueStatus.SUCCESS.code(), 0, BUCKET, Unpooled.EMPTY_BUFFER, null, null);

    private static void assertBuffersFreed(List<ByteBuf> buffers) {
        for (ByteBuf buffer : buffers) {
            assertEquals(0, buffer.refCnt());
        }
    }

    @Test
    public void shouldFlushBucket() {
        ClusterFacade core = mock(ClusterFacade.class);

        List<ByteBuf> upsertBuffers = new ArrayList<ByteBuf>();
        for (int i = 0; i < BucketFlusher.FLUSH_MARKER_SIZE; i++) {
            upsertBuffers.add(Unpooled.buffer());
        }
        final Iterator<ByteBuf> upsertIterator = upsertBuffers.iterator();
        when(core.send(isA(UpsertRequest.class))).thenAnswer(new Answer<Observable<CouchbaseResponse>>() {
            @Override
            public Observable<CouchbaseResponse> answer(InvocationOnMock invocation) throws Throwable {
                return Observable.just(
                        (CouchbaseResponse) new UpsertResponse(ResponseStatus.SUCCESS, KeyValueStatus.SUCCESS.code(), 0, BUCKET,
                                upsertIterator.next(), null, null)
                );
            }
        });
        when(core.send(isA(FlushRequest.class))).thenReturn(Observable.just(GOOD_FLUSH_RESPONSE));

        Observable<Boolean> flushResult = BucketFlusher.flush(core, BUCKET, PASSWORD);
        assertTrue(flushResult.toBlocking().single());
        assertBuffersFreed(upsertBuffers);
    }

    @Test
    public void shouldPollIfNotDoneImmediately() {
        ClusterFacade core = mock(ClusterFacade.class);

        List<ByteBuf> upsertBuffers = new ArrayList<ByteBuf>();
        for (int i = 0; i < BucketFlusher.FLUSH_MARKER_SIZE; i++) {
            upsertBuffers.add(Unpooled.buffer());
        }
        final Iterator<ByteBuf> upsertIterator = upsertBuffers.iterator();
        when(core.send(isA(UpsertRequest.class))).thenAnswer(new Answer<Observable<CouchbaseResponse>>() {
            @Override
            public Observable<CouchbaseResponse> answer(InvocationOnMock invocation) throws Throwable {
                return Observable.just(
                        (CouchbaseResponse) new UpsertResponse(ResponseStatus.SUCCESS, KeyValueStatus.SUCCESS.code(),
                                0, BUCKET, upsertIterator.next(), null, null)
                );
            }
        });

        when(core.send(isA(FlushRequest.class))).thenReturn(Observable.just(PEND_FLUSH_RESPONSE));

        List<ByteBuf> getBuffers = new ArrayList<ByteBuf>();
        for (int i = 0; i < BucketFlusher.FLUSH_MARKER_SIZE; i++) {
            getBuffers.add(Unpooled.buffer());
        }
        final Iterator<ByteBuf> getIterator = getBuffers.iterator();

        when(core.send(isA(GetRequest.class))).thenAnswer(new Answer<Observable<CouchbaseResponse>>() {
            @Override
            public Observable<CouchbaseResponse> answer(InvocationOnMock invocation) throws Throwable {
                return Observable.just(
                        (CouchbaseResponse) new GetResponse(ResponseStatus.NOT_EXISTS, KeyValueStatus.SUCCESS.code(),
                                0, 0, BUCKET, getIterator.next(), null)
                );
            }
        });

        Observable<Boolean> flushResult = BucketFlusher.flush(core, BUCKET, PASSWORD);
        assertTrue(flushResult.toBlocking().single());
        assertBuffersFreed(upsertBuffers);
        assertBuffersFreed(getBuffers);
    }

    @Test
    public void shouldPollAsLongAsNeeded() {
        ClusterFacade core = mock(ClusterFacade.class);

        when(core.send(isA(UpsertRequest.class))).thenReturn(Observable.just(GOOD_UPSERT_RESPONSE));
        when(core.send(isA(FlushRequest.class))).thenReturn(Observable.just(PEND_FLUSH_RESPONSE));

        // We pretend that 2 * 1024 + 33 docs are pending (so 2 times flush is fully in progress and once it is
        // partially done before completely being done.
        final AtomicInteger docsStillExisting = new AtomicInteger(1024 * 2 + 33);
        when(core.send(isA(GetRequest.class))).thenAnswer(new Answer<Observable<CouchbaseResponse>>() {
            @Override
            public Observable<CouchbaseResponse> answer(InvocationOnMock invocation) throws Throwable {
                ResponseStatus status = docsStillExisting.getAndDecrement() > 0
                        ? ResponseStatus.SUCCESS : ResponseStatus.NOT_EXISTS;
                short statusCode = status == ResponseStatus.SUCCESS ? KeyValueStatus.SUCCESS.code() : KeyValueStatus.ERR_NOT_FOUND.code();
                return Observable.just(
                        (CouchbaseResponse) new GetResponse(status, statusCode, 0, 0, BUCKET, Unpooled.EMPTY_BUFFER, null)
                );
            }
        });

        Observable<Boolean> flushResult = BucketFlusher.flush(core, BUCKET, PASSWORD);
        assertTrue(flushResult.toBlocking().single());
    }

    @Test(expected = FlushDisabledException.class)
    public void shouldFailIfFlushDisabled() {
        ClusterFacade core = mock(ClusterFacade.class);

        when(core.send(isA(UpsertRequest.class))).thenReturn(Observable.just(GOOD_UPSERT_RESPONSE));
        when(core.send(isA(FlushRequest.class))).thenReturn(Observable.just(
                (CouchbaseResponse) new FlushResponse(true, "disabled", ResponseStatus.FAILURE)
        ));
        BucketFlusher.flush(core, BUCKET, PASSWORD).toBlocking().single();
    }

    @Test(expected = CouchbaseException.class)
    public void shouldFailOnOtherError() {
        ClusterFacade core = mock(ClusterFacade.class);

        when(core.send(isA(UpsertRequest.class))).thenReturn(Observable.just(GOOD_UPSERT_RESPONSE));
        when(core.send(isA(FlushRequest.class))).thenReturn(Observable.just(
                (CouchbaseResponse) new FlushResponse(true, "wooops", ResponseStatus.FAILURE)
        ));
        BucketFlusher.flush(core, BUCKET, PASSWORD).toBlocking().single();
    }
}
