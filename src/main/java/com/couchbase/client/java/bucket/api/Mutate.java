/*
 * Copyright (c) 2018 Couchbase, Inc.
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
package com.couchbase.client.java.bucket.api;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.message.CouchbaseRequest;
import com.couchbase.client.core.message.kv.InsertRequest;
import com.couchbase.client.core.message.kv.InsertResponse;
import com.couchbase.client.core.message.kv.ReplaceRequest;
import com.couchbase.client.core.message.kv.ReplaceResponse;
import com.couchbase.client.core.message.kv.UpsertRequest;
import com.couchbase.client.core.message.kv.UpsertResponse;
import com.couchbase.client.core.tracing.ThresholdLogReporter;
import com.couchbase.client.core.tracing.ThresholdLogSpan;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.CouchbaseOutOfMemoryException;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.error.RequestTooBigException;
import com.couchbase.client.java.error.TemporaryFailureException;
import com.couchbase.client.java.transcoder.Transcoder;
import io.opentracing.Scope;
import io.opentracing.Span;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.couchbase.client.java.bucket.api.Utils.addDetails;
import static com.couchbase.client.java.bucket.api.Utils.applyTimeout;
import static com.couchbase.client.java.util.OnSubscribeDeferAndWatch.deferAndWatch;

/**
 * Contains the logic to execute and handle mutation requests.
 *
 * @author Michael Nitschinger
 * @since 2.6.0
 */
@InterfaceAudience.Private
@InterfaceStability.Uncommitted
public class Mutate {

    @SuppressWarnings({ "unchecked" })
    public static <D extends Document<?>> Observable<D> insert(final D document, final CouchbaseEnvironment env,
        final Transcoder<Document<Object>, Object> transcoder, final ClusterFacade core, final String bucket,
        final long timeout, final TimeUnit timeUnit) {
        return Observable.defer(new Func0<Observable<D>>() {
            final AtomicReference<CouchbaseRequest> r = new AtomicReference<CouchbaseRequest>();
            @Override
            public Observable<D> call() {
                return applyTimeout(deferAndWatch(new Func1<Subscriber, Observable<InsertResponse>>() {
                    @Override
                    public Observable<InsertResponse> call(Subscriber s) {
                        Span requestSpan = null;
                        if (env.tracingEnabled()) {
                            Scope scope = env.tracer()
                                .buildSpan("insert")
                                .startActive(false);
                            requestSpan = scope.span();
                            scope.close();
                        }

                        Scope encodeScope = null;
                        if (requestSpan != null) {
                            encodeScope = env.tracer()
                                .buildSpan("request_encoding")
                                .asChildOf(requestSpan)
                                .startActive(true);
                        }

                        Tuple2<ByteBuf, Integer> encoded = transcoder.encode((Document<Object>) document);

                        if (encodeScope != null) {
                            encodeScope.close();
                            if (encodeScope.span() instanceof ThresholdLogSpan) {
                                encodeScope.span().setBaggageItem(ThresholdLogReporter.KEY_ENCODE_MICROS,
                                    Long.toString(((ThresholdLogSpan) encodeScope.span()).durationMicros())
                                );
                            }
                        }

                        InsertRequest request = new InsertRequest(
                            document.id(), encoded.value1(), document.expiry(), encoded.value2(), bucket
                        );
                        if (requestSpan != null) {
                            request.span(requestSpan, env);
                        }
                        request.subscriber(s);
                        return core.send(request);
                    }
                }).map(new Func1<InsertResponse, D>() {
                    @Override
                    public D call(InsertResponse response) {
                        if (response.content() != null && response.content().refCnt() > 0) {
                            response.content().release();
                        }

                        if (env.tracingEnabled()) {
                            env.tracer().scopeManager()
                                .activate(response.request().span(), true)
                                .close();
                        }

                        if (response.status().isSuccess()) {
                            return (D) transcoder.newDocument(document.id(), document.expiry(),
                                document.content(), response.cas(), response.mutationToken());
                        }

                        switch (response.status()) {
                            case TOO_BIG:
                                throw addDetails(new RequestTooBigException(), response);
                            case EXISTS:
                                throw addDetails(new DocumentAlreadyExistsException(), response);
                            case TEMPORARY_FAILURE:
                            case SERVER_BUSY:
                                throw addDetails(new TemporaryFailureException(), response);
                            case OUT_OF_MEMORY:
                                throw addDetails(new CouchbaseOutOfMemoryException(), response);
                            default:
                                throw addDetails(new CouchbaseException(response.status().toString()), response);
                        }
                    }
                }), r, env, timeout, timeUnit);
            }
        });
    }

    @SuppressWarnings({ "unchecked" })
    public static <D extends Document<?>> Observable<D> upsert(final D document, final CouchbaseEnvironment env,
        final Transcoder<Document<Object>, Object> transcoder, final ClusterFacade core, final String bucket,
        final long timeout, final TimeUnit timeUnit) {
        return Observable.defer(new Func0<Observable<D>>() {
            @Override
            public Observable<D> call() {
                final AtomicReference<CouchbaseRequest> r = new AtomicReference<CouchbaseRequest>();
                return applyTimeout(deferAndWatch(new Func1<Subscriber, Observable<UpsertResponse>>() {
                    @Override
                    public Observable<UpsertResponse> call(Subscriber s) {
                        Span requestSpan = null;
                        if (env.tracingEnabled()) {
                            Scope scope = env.tracer()
                                .buildSpan("upsert")
                                .startActive(false);
                            requestSpan = scope.span();
                            scope.close();
                        }

                        Scope encodeScope = null;
                        if (requestSpan != null) {
                            encodeScope = env.tracer()
                                .buildSpan("request_encoding")
                                .asChildOf(requestSpan)
                                .startActive(true);
                        }

                        Tuple2<ByteBuf, Integer> encoded = transcoder.encode((Document<Object>) document);

                        if (encodeScope != null) {
                            encodeScope.close();
                            if (encodeScope.span() instanceof ThresholdLogSpan) {
                                encodeScope.span().setBaggageItem(ThresholdLogReporter.KEY_ENCODE_MICROS,
                                    Long.toString(((ThresholdLogSpan) encodeScope.span()).durationMicros())
                                );
                            }
                        }

                        UpsertRequest request = new UpsertRequest(
                            document.id(), encoded.value1(), document.expiry(), encoded.value2(), bucket
                        );
                        r.set(request);
                        if (requestSpan != null) {
                            request.span(requestSpan, env);
                        }
                        request.subscriber(s);
                        return core.send(request);
                    }
                }).map(new Func1<UpsertResponse, D>() {
                    @Override
                    public D call(UpsertResponse response) {
                        if (response.content() != null && response.content().refCnt() > 0) {
                            response.content().release();
                        }

                        if (env.tracingEnabled()) {
                            env.tracer().scopeManager()
                                .activate(response.request().span(), true)
                                .close();
                        }

                        if (response.status().isSuccess()) {
                            return (D) transcoder.newDocument(document.id(), document.expiry(),
                                document.content(), response.cas(), response.mutationToken());
                        }

                        switch (response.status()) {
                            case TOO_BIG:
                                throw addDetails(new RequestTooBigException(), response);
                            case EXISTS:
                            case LOCKED:
                                throw addDetails(new CASMismatchException(), response);
                            case TEMPORARY_FAILURE:
                            case SERVER_BUSY:
                                throw addDetails(new TemporaryFailureException(), response);
                            case OUT_OF_MEMORY:
                                throw addDetails(new CouchbaseOutOfMemoryException(), response);
                            default:
                                throw addDetails(new CouchbaseException(response.status().toString()), response);
                        }
                    }
                }), r, env, timeout, timeUnit);
            }
        });
    }

    @SuppressWarnings({ "unchecked" })
    public static <D extends Document<?>> Observable<D> replace(final D document, final CouchbaseEnvironment env,
                                                                final Transcoder<Document<Object>, Object> transcoder, final ClusterFacade core, final String bucket,
                                                                final long timeout, final TimeUnit timeUnit) {
        return Observable.defer(new Func0<Observable<D>>() {
            @Override
            public Observable<D> call() {
                final AtomicReference<CouchbaseRequest> r = new AtomicReference<CouchbaseRequest>();
                return applyTimeout(deferAndWatch(new Func1<Subscriber, Observable<ReplaceResponse>>() {
                    @Override
                    public Observable<ReplaceResponse> call(Subscriber s) {
                        Span requestSpan = null;
                        if (env.tracingEnabled()) {
                            Scope scope = env.tracer()
                                .buildSpan("replace")
                                .startActive(false);
                            requestSpan = scope.span();
                            scope.close();
                        }

                        Scope encodeScope = null;
                        if (requestSpan != null) {
                            encodeScope = env.tracer()
                                .buildSpan("request_encoding")
                                .asChildOf(requestSpan)
                                .startActive(true);
                        }

                        Tuple2<ByteBuf, Integer> encoded = transcoder.encode((Document<Object>) document);

                        if (encodeScope != null) {
                            encodeScope.close();
                            if (encodeScope.span() instanceof ThresholdLogSpan) {
                                encodeScope.span().setBaggageItem(ThresholdLogReporter.KEY_ENCODE_MICROS,
                                    Long.toString(((ThresholdLogSpan) encodeScope.span()).durationMicros())
                                );
                            }
                        }

                        ReplaceRequest request = new ReplaceRequest(
                            document.id(), encoded.value1(), document.cas(), document.expiry(), encoded.value2(), bucket
                        );
                        if (requestSpan != null) {
                            request.span(requestSpan, env);
                        }
                        request.subscriber(s);
                        return core.send(request);
                    }
                }).map(new Func1<ReplaceResponse, D>() {
                    @Override
                    public D call(ReplaceResponse response) {
                        if (response.content() != null && response.content().refCnt() > 0) {
                            response.content().release();
                        }

                        if (env.tracingEnabled()) {
                            env.tracer().scopeManager()
                                .activate(response.request().span(), true)
                                .close();
                        }

                        if (response.status().isSuccess()) {
                            return (D) transcoder.newDocument(document.id(), document.expiry(),
                                document.content(), response.cas(), response.mutationToken());
                        }

                        switch (response.status()) {
                            case TOO_BIG:
                                throw addDetails(new RequestTooBigException(), response);
                            case NOT_EXISTS:
                                throw addDetails(new DocumentDoesNotExistException(), response);
                            case EXISTS:
                            case LOCKED:
                                throw addDetails(new CASMismatchException(), response);
                            case TEMPORARY_FAILURE:
                            case SERVER_BUSY:
                                throw addDetails(new TemporaryFailureException(), response);
                            case OUT_OF_MEMORY:
                                throw addDetails(new CouchbaseOutOfMemoryException(), response);
                            default:
                                throw addDetails(new CouchbaseException(response.status().toString()), response);
                        }
                    }
                }), r, env, timeout, timeUnit);
            }
        });
    }
}
