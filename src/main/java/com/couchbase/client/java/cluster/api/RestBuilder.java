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
package com.couchbase.client.java.cluster.api;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.message.config.RestApiRequest;
import com.couchbase.client.core.message.config.RestApiResponse;
import com.couchbase.client.core.utils.Blocking;
import com.couchbase.client.deps.io.netty.handler.codec.http.HttpHeaders;
import com.couchbase.client.deps.io.netty.handler.codec.http.HttpMethod;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.document.json.JsonValue;
import com.couchbase.client.java.env.CouchbaseEnvironment;

/**
 * A builder class to incrementally construct REST API requests and execute
 * them synchronously
 *
 * @author Simon Baslé
 * @since 2.3.2
 */
@InterfaceAudience.Public
@InterfaceStability.Experimental
public class RestBuilder implements RestBuilderMarker {

    private final AsyncRestBuilder delegate;
    private final long defaultTimeout;
    private final TimeUnit defaultTimeUnit;

    /**
     * @param asyncBuilder
     * @param defaultTimeout
     * @param defaultTimeUnit
     */
    public RestBuilder(AsyncRestBuilder asyncBuilder, long defaultTimeout, TimeUnit defaultTimeUnit) {
        this.delegate = asyncBuilder;
        this.defaultTimeout = defaultTimeout;
        this.defaultTimeUnit = defaultTimeUnit;
    }

    /**
     * Adds an URL query parameter to the request. Using a key twice will
     * result in the last call being taken into account.
     *  @param key the parameter key.
     * @param value the parameter value.
     */
    public RestBuilder withParam(String key, String value) {
        delegate.withParam(key, value);
        return this;
    }

    /**
     * Sets the "Content-Type" standard header's value. This is a convenience
     * method equivalent to calling
     * {@link #withHeader(String, Object) withHeader("Content-Type", type)}.
     *
     * @param type the "Content-Type" to use.
     */
    public RestBuilder contentType(String type) {
        delegate.contentType(type);
        return this;
    }

    /**
     * Adds an HTTP header to the request. Using a key twice will result
     * in the last value being used for a given header.
     *
     * @param key the header name (see HttpHeaders.Names for standard names).
     * @param value the header value (see HttpHeaders.Values for standard values).
     */
    public RestBuilder withHeader(String key, Object value) {
        delegate.withHeader(key, value);
        return this;
    }

    /**
     * Sets the body for the request, assuming it is JSON. This is equivalent to setting
     * the {@link #contentType(String) "Content-Type"} to <code>"application/json"</code>
     * and then setting the body via {@link #bodyRaw(String)}.
     *
     * Note that you should avoid calling this for HTTP methods where it makes no sense
     * (eg. GET, DELETE), as it won't be ignored for these types of requests.
     *
     * @param jsonBody the JSON body to use, as a String.
     */
    public RestBuilder body(String jsonBody) {
        delegate.body(jsonBody);
        return this;
    }

    /**
     * Sets the body for the request, assuming it is JSON. This is equivalent to setting
     * the {@link #contentType(String) "Content-Type"} to <code>"application/json"</code>
     * and then setting the body via {@link #bodyRaw(String)}.
     *
     * Note that you should avoid calling this for HTTP methods where it makes no sense
     * (eg. GET, DELETE), as it won't be ignored for these types of requests.
     *
     * @param jsonBody the JSON body to use, as a {@link JsonObject}.
     */
    public RestBuilder body(JsonValue jsonBody) {
        delegate.body(jsonBody);
        return this;
    }

    /**
     * Sets the body for the request without assuming a Content-Type or Accept header.
     * Note that you should avoid calling this for HTTP methods where it makes no sense
     * (eg. GET, DELETE), as it won't be ignored for these types of requests.
     *
     * @param body the raw body value to use, as a String.
     */
    public RestBuilder bodyRaw(String body) {
        delegate.bodyRaw(body);
        return this;
    }

    /**
     * Sets the "Accept" standard header's value. This is a convenience
     * method equivalent to calling {@link #withHeader(String, Object) withHeader("Accept", type)}.
     *
     * @param type the "Accept" type to use.
     */
    public RestBuilder accept(String type) {
        delegate.accept(type);
        return this;
    }

    /**
     * Sets the body for the request to be an url-encoded form. This is equivalent to setting
     * the {@link #contentType(String) "Content-Type"} to <code>"application/x-www-form-urlencoded"</code>
     * and then setting the body via {@link #bodyRaw(String)}.
     *
     * @param form the {@link Form} builder object used to set form parameters.
     */
    public RestBuilder bodyForm(Form form) {
        delegate.bodyForm(form);
        return this;
    }

    //==== DELEGATE Getters ====
    /**
     * @return the {@link HttpMethod} used for this request.
     */
    public HttpMethod method() {
        return delegate.method();
    }

    /**
     * @return the full HTTP path (minus query parameters) used for this request.
     */
    public String path() {
        return delegate.path();
    }

    /**
     * @return a copy of the query parameters used for this request.
     */
    public Map<String, String> params() {
        return delegate.params();
    }

    /**
     * @return a copy of the HTTP headers used for this request.
     */
    public Map<String, Object> headers() {
        return delegate.headers();
    }

    /**
     * @return the body used for this request.
     */
    public String body() {
        return delegate.body();
    }

    //==== RestApiRequest and execution ====
    /**
     * @return the {@link RestApiRequest} message sent through the {@link ClusterFacade}
     * when executing this request.
     */
    public RestApiRequest asRequest() {
        return delegate.asRequest();
    }

    /**
     * Executes the API request in a synchronous fashion, using the given timeout.
     *
     * @param timeout the custom timeout to use for the request.
     * @param timeUnit the {@link TimeUnit} for the timeout.
     * @return the result of the API call, as a {@link RestApiResponse}.
     * @throws RuntimeException wrapping a {@link TimeoutException} in case the request took too long.
     */
    public RestApiResponse execute(long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(delegate.execute(), timeout, timeUnit);
    }

    /**
     * Executes the API request in a synchronous fashion, using the default timeout.
     *
     * The default timeout is currently the same as the {@link CouchbaseEnvironment#viewTimeout() view timeout}.
     *
     * @return the result of the API call, as a {@link RestApiResponse}.
     * @throws RuntimeException wrapping a {@link TimeoutException} in case the request took too long.
     */
    public RestApiResponse execute() {
        return execute(defaultTimeout, defaultTimeUnit);
    }
}
