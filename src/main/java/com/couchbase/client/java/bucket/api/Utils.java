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

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.message.CouchbaseRequest;
import com.couchbase.client.core.message.CouchbaseResponse;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import io.opentracing.Scope;
import io.opentracing.Span;
import rx.Observable;
import rx.functions.Func1;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@InterfaceAudience.Private
@InterfaceStability.Uncommitted
public class Utils {

    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(Utils.class);

    /**
     * Helper method to encapsulate the logic of enriching the exception with detailed status info.
     */
    @InterfaceAudience.Private
    @InterfaceStability.Uncommitted
    public static <X extends CouchbaseException, R extends CouchbaseResponse> X addDetails(X ex, R r) {
        if (r.statusDetails() != null) {
            ex.details(r.statusDetails());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{} returned with enhanced error details {}", r, ex);
            }
        }
        return ex;
    }

    public static String formatTimeout(CouchbaseRequest request, long timeout) {
        return "localId: " + request.lastLocalId() + ", opId" + request.operationId() + ", local: "
            + request.lastLocalSocket()
            + ", remote: " + request.lastRemoteSocket() + ", timeout: " + timeout + "us";
    }

    public static <T> Observable<T> applyTimeout(final Observable<T> input, final CouchbaseRequest request,
        final CouchbaseEnvironment environment, final long timeout, final TimeUnit timeUnit) {
        if (timeout > 0) {
            return input
                .timeout(timeout, timeUnit, environment.scheduler())
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends T>>() {
                    @Override
                    public Observable<? extends T> call(Throwable t) {
                        if (t instanceof TimeoutException) {
                            return Observable.error(new TimeoutException(Utils.formatTimeout(
                                request,
                                timeUnit.toMicros(timeout)
                            )));
                        } else {
                            return Observable.error(t);
                        }
                    }
                });
        } else {
            return input;
        }
    }

    public static void addRequestSpan(CouchbaseEnvironment env, CouchbaseRequest request, String opName) {
        if (env.tracingEnabled()) {
            Scope scope = env.tracer()
                .buildSpan(opName)
                .startActive(false);
            request.span(scope.span(), env);
            scope.close();
        }
    }

    public static void addRequestSpanWithParent(CouchbaseEnvironment env, Span parent, CouchbaseRequest request,
        String opName) {
        if (env.tracingEnabled()) {
            Scope scope = env.tracer()
                .buildSpan(opName)
                .asChildOf(parent)
                .startActive(false);
            request.span(scope.span(), env);
            scope.close();
        }
    }

}
