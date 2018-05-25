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
import com.couchbase.client.core.message.kv.AppendRequest;
import com.couchbase.client.core.message.kv.AppendResponse;
import com.couchbase.client.core.message.kv.CounterRequest;
import com.couchbase.client.core.message.kv.CounterResponse;
import com.couchbase.client.core.message.kv.InsertRequest;
import com.couchbase.client.core.message.kv.InsertResponse;
import com.couchbase.client.core.message.kv.PrependRequest;
import com.couchbase.client.core.message.kv.PrependResponse;
import com.couchbase.client.core.message.kv.RemoveRequest;
import com.couchbase.client.core.message.kv.RemoveResponse;
import com.couchbase.client.core.message.kv.ReplaceRequest;
import com.couchbase.client.core.message.kv.ReplaceResponse;
import com.couchbase.client.core.message.kv.TouchRequest;
import com.couchbase.client.core.message.kv.TouchResponse;
import com.couchbase.client.core.message.kv.UnlockRequest;
import com.couchbase.client.core.message.kv.UnlockResponse;
import com.couchbase.client.core.message.kv.UpsertRequest;
import com.couchbase.client.core.message.kv.UpsertResponse;
import com.couchbase.client.core.tracing.ThresholdLogReporter;
import com.couchbase.client.core.tracing.ThresholdLogSpan;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonLongDocument;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.CouchbaseOutOfMemoryException;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.error.RequestTooBigException;
import com.couchbase.client.java.error.TemporaryFailureException;
import com.couchbase.client.java.error.TemporaryLockFailureException;
import com.couchbase.client.java.transcoder.Transcoder;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;

import java.util.concurrent.TimeUnit;

import static com.couchbase.client.java.CouchbaseAsyncBucket.COUNTER_NOT_EXISTS_EXPIRY;
import static com.couchbase.client.java.bucket.api.Utils.addDetails;
import static com.couchbase.client.java.bucket.api.Utils.addRequestSpan;
import static com.couchbase.client.java.bucket.api.Utils.addRequestSpanWithParent;
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
        final long timeout, final TimeUnit timeUnit, final Span parent) {
        return Observable.defer(new Func0<Observable<D>>() {
            @Override
            public Observable<D> call() {
                Span requestSpan = null;
                if (env.operationTracingEnabled()) {
                    Tracer.SpanBuilder spanBuilder = env.tracer()
                        .buildSpan("insert");
                    if (parent != null) {
                        spanBuilder = spanBuilder.asChildOf(parent);
                    }
                    Scope scope = spanBuilder.startActive(false);
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

                final InsertRequest request = new InsertRequest(
                    document.id(), encoded.value1(), document.expiry(), encoded.value2(), bucket
                );
                if (requestSpan != null) {
                    request.span(requestSpan, env);
                }
                return applyTimeout(deferAndWatch(new Func1<Subscriber, Observable<InsertResponse>>() {
                    @Override
                    public Observable<InsertResponse> call(Subscriber s) {
                        request.subscriber(s);
                        return core.send(request);
                    }
                }).map(new Func1<InsertResponse, D>() {
                    @Override
                    public D call(InsertResponse response) {
                        if (response.content() != null && response.content().refCnt() > 0) {
                            response.content().release();
                        }

                        if (env.operationTracingEnabled()) {
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
                }), request, env, timeout, timeUnit);
            }
        });
    }

    @SuppressWarnings({ "unchecked" })
    public static <D extends Document<?>> Observable<D> upsert(final D document, final CouchbaseEnvironment env,
        final Transcoder<Document<Object>, Object> transcoder, final ClusterFacade core, final String bucket,
        final long timeout, final TimeUnit timeUnit, final Span parent) {
        return Observable.defer(new Func0<Observable<D>>() {
            @Override
            public Observable<D> call() {
                Span requestSpan = null;
                if (env.operationTracingEnabled()) {
                    Tracer.SpanBuilder spanBuilder = env.tracer()
                        .buildSpan("upsert");
                    if (parent != null) {
                        spanBuilder = spanBuilder.asChildOf(parent);
                    }
                    Scope scope = spanBuilder.startActive(false);
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

                final UpsertRequest request = new UpsertRequest(
                    document.id(), encoded.value1(), document.expiry(), encoded.value2(), bucket
                );
                if (requestSpan != null) {
                    request.span(requestSpan, env);
                }

                return applyTimeout(deferAndWatch(new Func1<Subscriber, Observable<UpsertResponse>>() {
                    @Override
                    public Observable<UpsertResponse> call(Subscriber s) {
                        request.subscriber(s);
                        return core.send(request);
                    }
                }).map(new Func1<UpsertResponse, D>() {
                    @Override
                    public D call(UpsertResponse response) {
                        if (response.content() != null && response.content().refCnt() > 0) {
                            response.content().release();
                        }

                        if (env.operationTracingEnabled()) {
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
                }), request, env, timeout, timeUnit);
            }
        });
    }

    @SuppressWarnings({ "unchecked" })
    public static <D extends Document<?>> Observable<D> replace(final D document, final CouchbaseEnvironment env,
        final Transcoder<Document<Object>, Object> transcoder, final ClusterFacade core, final String bucket,
        final long timeout, final TimeUnit timeUnit, final Span parent) {
        return Observable.defer(new Func0<Observable<D>>() {
            @Override
            public Observable<D> call() {
                Span requestSpan = null;
                if (env.operationTracingEnabled()) {
                    Tracer.SpanBuilder spanBuilder = env.tracer()
                        .buildSpan("replace");
                    if (parent != null) {
                        spanBuilder = spanBuilder.asChildOf(parent);
                    }
                    Scope scope = spanBuilder.startActive(false);
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

                final ReplaceRequest request = new ReplaceRequest(
                    document.id(), encoded.value1(), document.cas(), document.expiry(), encoded.value2(), bucket
                );
                if (requestSpan != null) {
                    request.span(requestSpan, env);
                }
                return applyTimeout(deferAndWatch(new Func1<Subscriber, Observable<ReplaceResponse>>() {
                    @Override
                    public Observable<ReplaceResponse> call(Subscriber s) {
                        request.subscriber(s);
                        return core.send(request);
                    }
                }).map(new Func1<ReplaceResponse, D>() {
                    @Override
                    public D call(ReplaceResponse response) {
                        if (response.content() != null && response.content().refCnt() > 0) {
                            response.content().release();
                        }

                        if (env.operationTracingEnabled()) {
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
                }), request, env, timeout, timeUnit);
            }
        });
    }

    @SuppressWarnings({ "unchecked" })
    public static <D extends Document<?>> Observable<D> remove(final D document, final CouchbaseEnvironment env,
        final Transcoder<Document<Object>, Object> transcoder, final ClusterFacade core, final String bucket,
        final long timeout, final TimeUnit timeUnit, final Span parent) {

        return Observable.defer(new Func0<Observable<D>>() {
            @Override
            public Observable<D> call() {
                final RemoveRequest request = new RemoveRequest(document.id(), document.cas(), bucket);
                if (parent == null) {
                    addRequestSpan(env, request, "remove");
                } else {
                    addRequestSpanWithParent(env, parent, request, "remove");
                }
                return applyTimeout(deferAndWatch(new Func1<Subscriber, Observable<RemoveResponse>>() {
                    @Override
                    public Observable<RemoveResponse> call(Subscriber s) {
                        request.subscriber(s);
                        return core.send(request);
                    }
                }).map(new Func1<RemoveResponse, D>() {
                    @Override
                    public D call(final RemoveResponse response) {
                        if (response.content() != null && response.content().refCnt() > 0) {
                            response.content().release();
                        }

                        if (env.operationTracingEnabled()) {
                            env.tracer().scopeManager()
                                .activate(response.request().span(), true)
                                .close();
                        }

                        if (response.status().isSuccess()) {
                            return (D) transcoder.newDocument(document.id(), 0, null, response.cas(), response.mutationToken());
                        }

                        switch (response.status()) {
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
                }), request, env, timeout, timeUnit);
            }
        });
    }

    @SuppressWarnings({ "unchecked" })
    public static Observable<Boolean> unlock(final String id, final long cas,
        final CouchbaseEnvironment env, final ClusterFacade core, final String bucket,
        final long timeout, final TimeUnit timeUnit) {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                final UnlockRequest request = new UnlockRequest(id, cas, bucket);
                addRequestSpan(env, request, "unlock");
                return applyTimeout(deferAndWatch(new Func1<Subscriber, Observable<UnlockResponse>>() {
                    @Override
                    public Observable<UnlockResponse> call(Subscriber s) {
                        request.subscriber(s);
                        return core.send(request);
                    }
                }).map(new Func1<UnlockResponse, Boolean>() {
                    @Override
                    public Boolean call(UnlockResponse response) {
                        if (response.content() != null && response.content().refCnt() > 0) {
                            response.content().release();
                        }

                        if (env.operationTracingEnabled()) {
                            env.tracer().scopeManager()
                                .activate(response.request().span(), true)
                                .close();
                        }

                        if (response.status().isSuccess()) {
                            return true;
                        }

                        switch (response.status()) {
                            case NOT_EXISTS:
                                throw addDetails(new DocumentDoesNotExistException(), response);
                            case TEMPORARY_FAILURE:
                            case LOCKED:
                                throw addDetails(new TemporaryLockFailureException(), response);
                            case SERVER_BUSY:
                                throw addDetails(new TemporaryFailureException(), response);
                            case OUT_OF_MEMORY:
                                throw addDetails(new CouchbaseOutOfMemoryException(), response);
                            default:
                                throw addDetails(new CouchbaseException(response.status().toString()), response);
                        }
                    }
                }), request, env, timeout, timeUnit);
            }
        });
    }

    @SuppressWarnings({ "unchecked" })
    public static Observable<Boolean> touch(final String id, final int expiry,
        final CouchbaseEnvironment env, final ClusterFacade core, final String bucket,
        final long timeout, final TimeUnit timeUnit) {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                final TouchRequest request = new TouchRequest(id, expiry, bucket);
                addRequestSpan(env, request, "touch");
                return applyTimeout(deferAndWatch(new Func1<Subscriber, Observable<TouchResponse>>() {
                    @Override
                    public Observable<TouchResponse> call(Subscriber s) {
                        request.subscriber(s);
                        return core.send(request);
                    }
                }).map(new Func1<TouchResponse, Boolean>() {
                    @Override
                    public Boolean call(TouchResponse response) {
                        if (response.content() != null && response.content().refCnt() > 0) {
                            response.content().release();
                        }

                        if (env.operationTracingEnabled()) {
                            env.tracer().scopeManager()
                                .activate(response.request().span(), true)
                                .close();
                        }

                        if (response.status().isSuccess()) {
                            return true;
                        }

                        switch (response.status()) {
                            case NOT_EXISTS:
                                throw addDetails(new DocumentDoesNotExistException(), response);
                            case TEMPORARY_FAILURE:
                            case SERVER_BUSY:
                            case LOCKED:
                                throw addDetails(new TemporaryFailureException(), response);
                            case OUT_OF_MEMORY:
                                throw addDetails(new CouchbaseOutOfMemoryException(), response);
                            default:
                                throw addDetails(new CouchbaseException(response.status().toString()), response);
                        }
                    }
                }), request, env, timeout, timeUnit);
            }
        });
    }

    public static Observable<JsonLongDocument> counter(final String id, final long delta, final long initial,
        final int expiry, final CouchbaseEnvironment env, final ClusterFacade core,
        final String bucket, final long timeout, final TimeUnit timeUnit, final Span parent) {
        return Observable.defer(new Func0<Observable<JsonLongDocument>>() {
            @Override
            public Observable<JsonLongDocument> call() {
                final CounterRequest request = new CounterRequest(id, initial, delta, expiry, bucket);
                if (parent == null) {
                    addRequestSpan(env, request, "counter");
                } else {
                    addRequestSpanWithParent(env, parent, request, "counter");
                }
                return applyTimeout(deferAndWatch(new Func1<Subscriber, Observable<CounterResponse>>() {
                    @Override
                    public Observable<CounterResponse> call(Subscriber s) {
                        request.subscriber(s);
                        return core.send(request);
                    }
                }).map(new Func1<CounterResponse, JsonLongDocument>() {
                    @Override
                    public JsonLongDocument call(CounterResponse response) {
                        if (response.content() != null && response.content().refCnt() > 0) {
                            response.content().release();
                        }

                        if (env.operationTracingEnabled()) {
                            env.tracer().scopeManager()
                                .activate(response.request().span(), true)
                                .close();
                        }

                        if (response.status().isSuccess()) {
                            int returnedExpiry = expiry == COUNTER_NOT_EXISTS_EXPIRY ? 0 : expiry;
                            return JsonLongDocument.create(id, returnedExpiry, response.value(),
                                response.cas(), response.mutationToken());
                        }

                        switch (response.status()) {
                            case NOT_EXISTS:
                                throw addDetails(new DocumentDoesNotExistException(), response);
                            case TEMPORARY_FAILURE:
                            case SERVER_BUSY:
                            case LOCKED:
                                throw addDetails(new TemporaryFailureException(), response);
                            case OUT_OF_MEMORY:
                                throw addDetails(new CouchbaseOutOfMemoryException(), response);
                            default:
                                throw addDetails(new CouchbaseException(response.status().toString()), response);
                        }
                    }
                }), request, env, timeout, timeUnit);
            }
        });
    }

    @SuppressWarnings({ "unchecked" })
    public static <D extends Document<?>> Observable<D> append(final D document, final CouchbaseEnvironment env,
        final Transcoder<Document<Object>, Object> transcoder, final ClusterFacade core, final String bucket,
        final long timeout, final TimeUnit timeUnit, final Span parent) {
        return Observable.defer(new Func0<Observable<D>>() {
            @Override
            public Observable<D> call() {
                Span requestSpan = null;
                if (env.operationTracingEnabled()) {
                    Tracer.SpanBuilder spanBuilder = env.tracer()
                        .buildSpan("append");
                    if (parent != null) {
                        spanBuilder = spanBuilder.asChildOf(parent);
                    }
                    Scope scope = spanBuilder.startActive(false);
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

                final AppendRequest request = new AppendRequest(
                    document.id(), document.cas(), encoded.value1(), bucket
                );

                if (requestSpan != null) {
                    request.span(requestSpan, env);
                }
                return applyTimeout(deferAndWatch(new Func1<Subscriber, Observable<AppendResponse>>() {
                    @Override
                    public Observable<AppendResponse> call(Subscriber s) {
                        request.subscriber(s);
                        return core.send(request);
                    }
                }).map(new Func1<AppendResponse, D>() {
                    @Override
                    public D call(final AppendResponse response) {
                        if (response.content() != null && response.content().refCnt() > 0) {
                            response.content().release();
                        }

                        if (response.status().isSuccess()) {
                            return (D) transcoder.newDocument(document.id(), 0, null, response.cas(), response.mutationToken());
                        }

                        switch (response.status()) {
                            case TOO_BIG:
                                throw addDetails(new RequestTooBigException(), response);
                            case NOT_STORED:
                                throw addDetails(new DocumentDoesNotExistException(), response);
                            case TEMPORARY_FAILURE:
                            case SERVER_BUSY:
                            case LOCKED:
                                throw addDetails(new TemporaryFailureException(), response);
                            case OUT_OF_MEMORY:
                                throw addDetails(new CouchbaseOutOfMemoryException(), response);
                            case EXISTS:
                                throw addDetails(new CASMismatchException(), response);
                            default:
                                throw addDetails(new CouchbaseException(response.status().toString()), response);
                        }
                    }
                }), request, env, timeout, timeUnit);
            }
        });
    }

    @SuppressWarnings({ "unchecked" })
    public static <D extends Document<?>> Observable<D> prepend(final D document, final CouchbaseEnvironment env,
        final Transcoder<Document<Object>, Object> transcoder, final ClusterFacade core, final String bucket,
        final long timeout, final TimeUnit timeUnit, final Span parent) {
        return Observable.defer(new Func0<Observable<D>>() {
            @Override
            public Observable<D> call() {
                Span requestSpan = null;
                if (env.operationTracingEnabled()) {
                    Tracer.SpanBuilder spanBuilder = env.tracer()
                        .buildSpan("prepend");
                    if (parent != null) {
                        spanBuilder = spanBuilder.asChildOf(parent);
                    }
                    Scope scope = spanBuilder.startActive(false);
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

                final PrependRequest request = new PrependRequest(
                    document.id(), document.cas(), encoded.value1(), bucket
                );

                if (requestSpan != null) {
                    request.span(requestSpan, env);
                }
                return applyTimeout(deferAndWatch(new Func1<Subscriber, Observable<PrependResponse>>() {
                    @Override
                    public Observable<PrependResponse> call(Subscriber s) {
                        request.subscriber(s);
                        return core.send(request);
                    }
                }).map(new Func1<PrependResponse, D>() {
                    @Override
                    public D call(final PrependResponse response) {
                        if (response.content() != null && response.content().refCnt() > 0) {
                            response.content().release();
                        }

                        if (response.status().isSuccess()) {
                            return (D) transcoder.newDocument(document.id(), 0, null, response.cas(), response.mutationToken());
                        }

                        switch (response.status()) {
                            case TOO_BIG:
                                throw addDetails(new RequestTooBigException(), response);
                            case NOT_STORED:
                                throw addDetails(new DocumentDoesNotExistException(), response);
                            case TEMPORARY_FAILURE:
                            case SERVER_BUSY:
                            case LOCKED:
                                throw addDetails(new TemporaryFailureException(), response);
                            case OUT_OF_MEMORY:
                                throw addDetails(new CouchbaseOutOfMemoryException(), response);
                            case EXISTS:
                                throw addDetails(new CASMismatchException(), response);
                            default:
                                throw addDetails(new CouchbaseException(response.status().toString()), response);
                        }
                    }
                }), request, env, timeout, timeUnit);
            }
        });
    }
}
