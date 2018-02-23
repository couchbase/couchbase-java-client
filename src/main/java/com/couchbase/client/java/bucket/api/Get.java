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
import com.couchbase.client.core.message.kv.GetRequest;
import com.couchbase.client.core.message.kv.GetResponse;
import com.couchbase.client.core.tracing.ThresholdLogReporter;
import com.couchbase.client.core.tracing.ThresholdLogSpan;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.error.CouchbaseOutOfMemoryException;
import com.couchbase.client.java.error.TemporaryFailureException;
import com.couchbase.client.java.error.TemporaryLockFailureException;
import com.couchbase.client.java.transcoder.Transcoder;
import io.opentracing.Scope;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.couchbase.client.java.bucket.api.Utils.addDetails;
import static com.couchbase.client.java.bucket.api.Utils.addRequestSpan;
import static com.couchbase.client.java.bucket.api.Utils.applyTimeout;
import static com.couchbase.client.java.util.OnSubscribeDeferAndWatch.deferAndWatch;

/**
 * Contains the logic to execute and handle get requests.
 *
 * @author Michael Nitschinger
 * @since 2.6.0
 */
@InterfaceAudience.Private
@InterfaceStability.Uncommitted
public class Get {

    public static <D extends Document<?>> Observable<D> get(final String id, final Class<D> target,
        final CouchbaseEnvironment environment, final String bucket, final ClusterFacade core,
        final Map<Class<? extends Document>, Transcoder<? extends Document, ?>> transcoders,
        final long timeout, final TimeUnit timeUnit) {
        return deferAndWatch(new Func1<Subscriber, Observable<? extends D>>() {
            @Override
            public Observable<? extends D> call(Subscriber s) {
                final GetRequest request = new GetRequest(id, bucket);
                addRequestSpan(environment, request, "get");
                request.subscriber(s);
                Observable<D> result = core.<GetResponse>send(request)
                    .filter(new GetFilter(environment))
                    .map(new GetMap<D>(environment, transcoders, target, id));
                return applyTimeout(result, request, environment, timeout, timeUnit);
            }
        });
    }

    public static <D extends Document<?>> Observable<D> getAndLock(final String id, final Class<D> target,
        final CouchbaseEnvironment environment, final String bucket, final ClusterFacade core,
        final Map<Class<? extends Document>, Transcoder<? extends Document, ?>> transcoders, final int lockTime,
        final long timeout, final TimeUnit timeUnit) {
        return deferAndWatch(new Func1<Subscriber, Observable<? extends D>>() {
            @Override
            public Observable<? extends D> call(Subscriber s) {
                GetRequest request = new GetRequest(id, bucket, true, false, lockTime);
                addRequestSpan(environment, request, "get_and_lock");
                request.subscriber(s);
                Observable<D> result = core.<GetResponse>send(request)
                    .filter(new GetAndLockFilter(environment))
                    .map(new GetMap<D>(environment, transcoders, target, id));
                return applyTimeout(result, request, environment, timeout, timeUnit);
            }
        });
    }

    public static <D extends Document<?>> Observable<D> getAndTouch(final String id, final Class<D> target,
        final CouchbaseEnvironment environment, final String bucket, final ClusterFacade core,
        final Map<Class<? extends Document>, Transcoder<? extends Document, ?>> transcoders, final int expiry,
        final long timeout, final TimeUnit timeUnit) {
        return deferAndWatch(new Func1<Subscriber, Observable<? extends D>>() {
            @Override
            public Observable<? extends D> call(Subscriber s) {
                GetRequest request = new GetRequest(id, bucket, false, true, expiry);
                addRequestSpan(environment, request, "get_and_touch");
                request.subscriber(s);
                Observable<D> result = core.<GetResponse>send(request)
                    .filter(new GetAndTouchFilter(environment))
                    .map(new GetMap<D>(environment, transcoders, target, id));
                return applyTimeout(result, request, environment, timeout, timeUnit);
            }
        });
    }


    private static class GetFilter implements Func1<GetResponse, Boolean> {

        private final CouchbaseEnvironment environment;

        GetFilter(CouchbaseEnvironment environment) {
            this.environment = environment;
        }

        @Override
        public Boolean call(GetResponse response) {
            if (response.status().isSuccess()) {
                return true;
            }
            ByteBuf content = response.content();
            if (content != null && content.refCnt() > 0) {
                content.release();
            }

            if (environment.tracingEnabled()) {
                environment.tracer().scopeManager()
                    .activate(response.request().span(), true)
                    .close();
            }

            switch (response.status()) {
                case NOT_EXISTS:
                    return false;
                case TEMPORARY_FAILURE:
                case SERVER_BUSY:
                    throw addDetails(new TemporaryFailureException(), response);
                case OUT_OF_MEMORY:
                    throw addDetails(new CouchbaseOutOfMemoryException(), response);
                default:
                    throw addDetails(new CouchbaseException(response.status().toString()), response);
            }
        }
    }

    private static class GetAndLockFilter implements Func1<GetResponse, Boolean> {

        private final CouchbaseEnvironment environment;

        GetAndLockFilter(CouchbaseEnvironment environment) {
            this.environment = environment;
        }

        @Override
        public Boolean call(GetResponse response) {
            if (response.status().isSuccess()) {
                return true;
            }
            ByteBuf content = response.content();
            if (content != null && content.refCnt() > 0) {
                content.release();
            }

            if (environment.tracingEnabled()) {
                environment.tracer().scopeManager()
                    .activate(response.request().span(), true)
                    .close();
            }

            switch (response.status()) {
                case NOT_EXISTS:
                    return false;
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
    }

    private static class GetAndTouchFilter implements Func1<GetResponse, Boolean> {

        private final CouchbaseEnvironment environment;

        GetAndTouchFilter(CouchbaseEnvironment environment) {
            this.environment = environment;
        }

        @Override
        public Boolean call(GetResponse response) {
            if (response.status().isSuccess()) {
                return true;
            }
            ByteBuf content = response.content();
            if (content != null && content.refCnt() > 0) {
                content.release();
            }

            if (environment.tracingEnabled()) {
                environment.tracer().scopeManager()
                    .activate(response.request().span(), true)
                    .close();
            }

            switch (response.status()) {
                case NOT_EXISTS:
                    return false;
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
    }

    private static class GetMap<D> implements Func1<GetResponse, D> {

        private final CouchbaseEnvironment environment;
        private final Map<Class<? extends Document>, Transcoder<? extends Document, ?>> transcoders;
        private final Class<D> target;
        private final String id;

        GetMap(CouchbaseEnvironment environment, Map<Class<? extends Document>,
            Transcoder<? extends Document, ?>> transcoders, Class<D> target, String id) {
            this.environment = environment;
            this.transcoders = transcoders;
            this.target = target;
            this.id = id;
        }

        @Override
        @SuppressWarnings({"unchecked"})
        public D call(final GetResponse response) {
            Transcoder<?, Object> transcoder = (Transcoder<?, Object>) transcoders.get(target);

            Scope decodeScope = null;
            if (environment.tracingEnabled()) {
                decodeScope = environment.tracer()
                    .buildSpan("response_decoding")
                    .asChildOf(response.request().span())
                    .startActive(true);
            }

            D decoded = (D) transcoder.decode(id, response.content(), response.cas(), 0,
                response.flags(), response.status());

            if (environment.tracingEnabled() && decodeScope != null) {
                decodeScope.close();
                if (decodeScope.span() instanceof ThresholdLogSpan) {
                    decodeScope.span().setBaggageItem(ThresholdLogReporter.KEY_DECODE_MICROS,
                        Long.toString(((ThresholdLogSpan) decodeScope.span()).durationMicros())
                    );
                }
            }

            if (environment.tracingEnabled()) {
                environment.tracer().scopeManager()
                    .activate(response.request().span(), true)
                    .close();
            }

            return decoded;
        }

    }


}
