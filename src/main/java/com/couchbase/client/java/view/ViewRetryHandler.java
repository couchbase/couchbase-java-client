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

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.RequestCancelledException;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.message.view.ViewQueryResponse;
import rx.Observable;
import rx.functions.Func1;

import java.util.concurrent.TimeUnit;

/**
 * Generic View retry handler based on response code and value inspection.
 *
 * @author Michael Nitschinger
 * @since 2.0.2
 */
public class ViewRetryHandler {

    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(ViewRetryHandler.class);

    private static final ShouldRetryViewRequestException SHOULD_RETRY = new ShouldRetryViewRequestException();

    static {
        SHOULD_RETRY.setStackTrace(new StackTraceElement[]{});
    }

    private ViewRetryHandler() {}

    /**
     * Takes a {@link ViewQueryResponse}, verifies their status based on fixed criteria and resubscribes if needed.
     *
     * If it needs to be retried, the resubscription will happen after 10 milliseconds to give the underlying code
     * some time to recover.
     *
     * @param input the original response.
     * @return the good response which can be parsed, or a failing observable.
     */
    public static Observable<ViewQueryResponse> retryOnCondition(final Observable<ViewQueryResponse> input) {
        return input
            .flatMap(new Func1<ViewQueryResponse, Observable<ViewQueryResponse>>() {
                @Override
                public Observable<ViewQueryResponse> call(final ViewQueryResponse response) {
                    return passThroughOrThrow(response);
                }
            })
            .retryWhen(new Func1<Observable<? extends Throwable>, Observable<?>>() {
                @Override
                public Observable<?> call(Observable<? extends Throwable> observable) {
                    return observable
                        .flatMap(new Func1<Throwable, Observable<?>>() {
                            @Override
                            public Observable<?> call(Throwable throwable) {
                                if (throwable instanceof ShouldRetryViewRequestException
                                    || throwable instanceof RequestCancelledException) {
                                    return Observable.timer(10, TimeUnit.MILLISECONDS);
                                } else {
                                    return Observable.error(throwable);
                                }
                            }
                        });
                }
            })
            .last();
    }

    /**
     * Helper method which decides if the response is good to pass through or needs to be retried.
     *
     * @param response the response to look at.
     * @return the {@link ViewQueryResponse} if it can pass through or an error if it needs to be retried.
     */
    private static Observable<ViewQueryResponse> passThroughOrThrow(final ViewQueryResponse response) {
        final int responseCode = response.responseCode();
        if (responseCode == 200) {
            return Observable.just(response);
        }

        return response
            .error()
            .map(new Func1<String, ViewQueryResponse>() {
                @Override
                public ViewQueryResponse call(String error) {
                    if (shouldRetry(responseCode, error)) {
                        throw SHOULD_RETRY;
                    }
                    return response;
                }
            })
            .singleOrDefault(response);
    }

    /**
     * Analyses status codes and checks if a retry needs to happen.
     *
     * Some status codes are ambiguous, so their contents are inspected further.
     *
     * @param status the status code.
     * @param content the error body from the response.
     * @return true if retry is needed, false otherwise.
     */
    private static boolean shouldRetry(final int status, final String content) {
        switch (status) {
            case 200:
                return false;
            case 404:
                return analyse404Response(content);
            case 500:
                return analyse500Response(content);
            case 300:
            case 301:
            case 302:
            case 303:
            case 307:
            case 401:
            case 408:
            case 409:
            case 412:
            case 416:
            case 417:
            case 501:
            case 502:
            case 503:
            case 504:
                return true;
            default:
                LOGGER.info("Received a View HTTP response code ({}) I did not expect, not retrying.", status);
                return false;
        }
    }

    /**
     * Analyses the content of a 404 response to see if it is legible for retry.
     *
     * If the content contains ""reason":"missing"", it is a clear indication that the responding node
     * is unprovisioned and therefore it should be retried. All other cases indicate a provisioned node,
     * but the design document/view is not found, which should not be retried.
     *
     * @param content the parsed error content.
     * @return true if it needs to be retried, false otherwise.
     */
    private static boolean analyse404Response(final String content) {
        if (content.contains("\"reason\":\"missing\"")) {
            return true;
        }

        LOGGER.debug("Design document not found, error is {}", content);
        return false;
    }

    /**
     * Analyses the content of a 500 response to see if it is legible for retry.
     *
     * @param content the parsed error content.
     * @return true if it needs to be retried, false otherwise.
     */
    private static boolean analyse500Response(final String content) {
        if (content.contains("error") && content.contains("{not_found, missing_named_view}")) {
            LOGGER.debug("Design document not found, error is {}", content);
            return false;
        }
        if (content.contains("error") && content.contains("\"badarg\"")) {
            LOGGER.debug("Malformed view query");
            return false;
        }
        return true;
    }

    /**
     * Exception type indicating a view needs to be retried.
     */
    private static class ShouldRetryViewRequestException extends CouchbaseException { }

}
