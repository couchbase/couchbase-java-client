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
package com.couchbase.client.java;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import com.couchbase.client.core.CouchbaseCore;
import com.couchbase.client.core.endpoint.kv.KeyValueStatus;
import com.couchbase.client.core.message.CouchbaseRequest;
import com.couchbase.client.core.message.CouchbaseResponse;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.InsertRequest;
import com.couchbase.client.core.message.kv.InsertResponse;
import com.couchbase.client.core.message.kv.RemoveRequest;
import com.couchbase.client.core.message.kv.RemoveResponse;
import com.couchbase.client.core.message.kv.ReplaceRequest;
import com.couchbase.client.core.message.kv.ReplaceResponse;
import com.couchbase.client.core.message.kv.UpsertRequest;
import com.couchbase.client.core.message.kv.UpsertResponse;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.transcoder.Transcoder;
import io.opentracing.Span;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import rx.Observable;
import rx.plugins.RxJavaHooks;
import rx.subjects.AsyncSubject;
import rx.subjects.Subject;

/**
 * Verifies functionality of the {@link CouchbaseAsyncBucket}.
 *
 * @author Michael Nitschinger
 * @since 2.1.1
 */
public class CouchbaseAsyncBucketTest {

    private static final CouchbaseEnvironment ENV = DefaultCouchbaseEnvironment.create();

    @AfterClass
    public static void tearDown() {
        ENV.shutdown();
    }

    @Test
    public void shouldNotCallIntoObserveOnInsertWhenNotNeeded() {
        CouchbaseCore core = mock(CouchbaseCore.class);
        CouchbaseAsyncBucket bucket = new CouchbaseAsyncBucket(
                core, ENV, "bucket", "", Collections.<Transcoder<? extends Document, ?>>emptyList()
        );

        final CouchbaseRequest requestMock = mock(CouchbaseRequest.class);
        when(requestMock.span()).thenReturn(mock(Span.class));
        Subject<CouchbaseResponse, CouchbaseResponse> response = AsyncSubject.create();
        response.onNext(new InsertResponse(
            ResponseStatus.SUCCESS, KeyValueStatus.SUCCESS.code(), 1234, "bucket", Unpooled.EMPTY_BUFFER,
            null, requestMock
        ));
        response.onCompleted();

        when(core.send(any(InsertRequest.class))).thenReturn(response);

        JsonDocument doc = JsonDocument.create("foo");
        Observable<JsonDocument> result = bucket.insert(doc, PersistTo.NONE, ReplicateTo.NONE);
        result.toBlocking().single();

        verify(core, times(1)).send(any(CouchbaseRequest.class));
    }

    @Test
    public void shouldNotCallIntoObserveOnUpsertWhenNotNeeded() {
        CouchbaseCore core = mock(CouchbaseCore.class);
        CouchbaseAsyncBucket bucket = new CouchbaseAsyncBucket(
                core, ENV, "bucket", "", Collections.<Transcoder<? extends Document, ?>>emptyList()
        );

        final CouchbaseRequest requestMock = mock(CouchbaseRequest.class);
        when(requestMock.span()).thenReturn(mock(Span.class));
        Subject<CouchbaseResponse, CouchbaseResponse> response = AsyncSubject.create();
        response.onNext(new UpsertResponse(
            ResponseStatus.SUCCESS, KeyValueStatus.SUCCESS.code(), 1234, "bucket", Unpooled.EMPTY_BUFFER, null,
            requestMock
        ));
        response.onCompleted();

        when(core.send(any(UpsertRequest.class))).thenReturn(response);

        JsonDocument doc = JsonDocument.create("foo");
        Observable<JsonDocument> result = bucket.upsert(doc, PersistTo.NONE, ReplicateTo.NONE);
        result.toBlocking().single();

        verify(core, times(1)).send(any(CouchbaseRequest.class));
    }

    @Test
    public void shouldNotCallIntoObserveOnReplaceWhenNotNeeded() {
        CouchbaseCore core = mock(CouchbaseCore.class);
        CouchbaseAsyncBucket bucket = new CouchbaseAsyncBucket(
                core, ENV, "bucket", "", Collections.<Transcoder<? extends Document, ?>>emptyList()
        );

        final CouchbaseRequest requestMock = mock(CouchbaseRequest.class);
        when(requestMock.span()).thenReturn(mock(Span.class));
        Subject<CouchbaseResponse, CouchbaseResponse> response = AsyncSubject.create();
        response.onNext(new ReplaceResponse(
            ResponseStatus.SUCCESS, KeyValueStatus.SUCCESS.code(), 1234, "bucket", Unpooled.EMPTY_BUFFER, null,
            requestMock
        ));
        response.onCompleted();

        when(core.send(any(ReplaceRequest.class))).thenReturn(response);

        JsonDocument doc = JsonDocument.create("foo");
        Observable<JsonDocument> result = bucket.replace(doc, PersistTo.NONE, ReplicateTo.NONE);
        result.toBlocking().single();

        verify(core, times(1)).send(any(CouchbaseRequest.class));
    }

    @Test
    public void shouldNotCallIntoObserveOnRemoveWhenNotNeeded() {
        CouchbaseCore core = mock(CouchbaseCore.class);
        CouchbaseAsyncBucket bucket = new CouchbaseAsyncBucket(
                core, ENV, "bucket", "", Collections.<Transcoder<? extends Document, ?>>emptyList()
        );

        final CouchbaseRequest requestMock = mock(CouchbaseRequest.class);
        when(requestMock.span()).thenReturn(mock(Span.class));
        Subject<CouchbaseResponse, CouchbaseResponse> response = AsyncSubject.create();
        response.onNext(new RemoveResponse(
            ResponseStatus.SUCCESS, KeyValueStatus.SUCCESS.code(), 1234, "bucket", Unpooled.EMPTY_BUFFER, null, requestMock
        ));
        response.onCompleted();

        when(core.send(any(RemoveRequest.class))).thenReturn(response);

        JsonDocument doc = JsonDocument.create("foo");
        Observable<JsonDocument> result = bucket.remove(doc, PersistTo.NONE, ReplicateTo.NONE);
        result.toBlocking().single();

        verify(core, times(1)).send(any(CouchbaseRequest.class));
    }

}
