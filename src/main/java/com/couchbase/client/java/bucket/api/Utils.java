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
import com.couchbase.client.core.message.analytics.AnalyticsRequest;
import com.couchbase.client.core.message.config.ConfigRequest;
import com.couchbase.client.core.message.kv.BinaryRequest;
import com.couchbase.client.core.message.query.QueryRequest;
import com.couchbase.client.core.message.search.SearchRequest;
import com.couchbase.client.core.message.view.ViewRequest;
import com.couchbase.client.core.tracing.ThresholdLogReporter;
import com.couchbase.client.core.utils.DefaultObjectMapper;
import com.couchbase.client.deps.com.fasterxml.jackson.core.JsonProcessingException;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import io.opentracing.Scope;
import io.opentracing.Span;
import rx.Observable;
import rx.functions.Func1;

import java.util.HashMap;
import java.util.Map;
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

    /**
     * This method take the given request and produces the correct additional timeout
     * information according to the RFC.
     */
    static String formatTimeout(final CouchbaseRequest request, final long timeout) {
        Map<String, Object> fieldMap = new HashMap<String, Object>();
        fieldMap.put("t", timeout);

        if (request != null) {
            fieldMap.put("s", formatServiceType(request));
            putIfNotNull(fieldMap, "i", request.operationId());
            putIfNotNull(fieldMap, "b", request.bucket());
            putIfNotNull(fieldMap, "c", request.lastLocalId());
            putIfNotNull(fieldMap, "l", request.lastLocalSocket());
            putIfNotNull(fieldMap, "r", request.lastRemoteSocket());
        }

        try {
            return DefaultObjectMapper.writeValueAsString(fieldMap);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Could not format timeout information for request " + request, e);
            return null;
        }
    }

    /**
     * Helper method to avoid ugly if/else blocks in {@link #formatTimeout(CouchbaseRequest, long)}.
     */
    private static void putIfNotNull(final Map<String, Object> map, final String key, final Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    /**
     * Helper method to turn the request into the proper string service type.
     */
    private static String formatServiceType(final CouchbaseRequest request) {
        if (request instanceof BinaryRequest) {
            return ThresholdLogReporter.SERVICE_KV;
        } else if (request instanceof QueryRequest) {
            return ThresholdLogReporter.SERVICE_N1QL;
        } else if (request instanceof ViewRequest) {
            return ThresholdLogReporter.SERVICE_VIEW;
        } else if (request instanceof AnalyticsRequest) {
            return ThresholdLogReporter.SERVICE_ANALYTICS;
        } else if (request instanceof SearchRequest) {
            return ThresholdLogReporter.SERVICE_FTS;
        } else if (request instanceof ConfigRequest) {
            // Shouldn't be user visible, but just for completeness sake.
            return "config";
        } else {
            return "unknown";
        }
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
        if (env.operationTracingEnabled()) {
            if (env.propagateParentSpan()) {
                Span potentialParent = env.tracer().activeSpan();
                if (potentialParent != null) {
                    // there is an active span, use it as a parent
                    addRequestSpanWithParent(env, potentialParent, request, opName);
                    return;
                }
            }

            Scope scope = env.tracer()
                .buildSpan(opName)
                .startActive(false);
            request.span(scope.span(), env);
            scope.close();
        }
    }

    public static void addRequestSpanWithParent(CouchbaseEnvironment env, Span parent, CouchbaseRequest request,
        String opName) {
        if (env.operationTracingEnabled()) {
            Scope scope = env.tracer()
                .buildSpan(opName)
                .asChildOf(parent)
                .startActive(false);
            request.span(scope.span(), env);
            scope.close();
        }
    }

}
