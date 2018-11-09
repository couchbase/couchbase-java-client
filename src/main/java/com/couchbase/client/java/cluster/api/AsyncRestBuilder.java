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

import java.util.LinkedHashMap;
import java.util.Map;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.message.config.RestApiRequest;
import com.couchbase.client.core.message.config.RestApiResponse;
import com.couchbase.client.deps.io.netty.handler.codec.http.HttpHeaders;
import com.couchbase.client.deps.io.netty.handler.codec.http.HttpMethod;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.document.json.JsonValue;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;

import static com.couchbase.client.java.util.OnSubscribeDeferAndWatch.deferAndWatch;

/**
 * A builder class to incrementally construct REST API requests and execute
 * them asynchronously
 *
 * @author Simon Baslé
 * @since 2.3.2
 */
@InterfaceAudience.Public
@InterfaceStability.Experimental
public class AsyncRestBuilder implements RestBuilderMarker {

    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(AsyncRestBuilder.class);

    //parameters from the RestApiClient
    private final String username;
    private final String password;
    private final ClusterFacade core;

    //internals of the builder
    private final HttpMethod method;
    private final String path;
    private final Map<String, String> params;
    private final Map<String, Object> headers;
    private String body;

    /**
     * @param username the username to authenticate the request with.
     * @param password the password to authenticate the request with.
     * @param core the {@link ClusterFacade core} through which to send the request.
     * @param method the {@link HttpMethod} for the request.
     * @param path the full URL path for the request.
     */
    public AsyncRestBuilder(String username, String password, ClusterFacade core, HttpMethod method, String path) {
        this.username = username;
        this.password = password;
        this.core = core;

        this.method = method;
        this.path = path;
        this.body = "";

        this.params = new LinkedHashMap<String, String>();
        this.headers = new LinkedHashMap<String, Object>();
    }

    /**
     * Adds an URL query parameter to the request. Using a key twice will
     * result in the last call being taken into account.
     *
     * @param key the parameter key.
     * @param value the parameter value.
     */
    public AsyncRestBuilder withParam(String key, String value) {
        this.params.put(key, value);
        return this;
    }

    /**
     * Adds an HTTP header to the request. Using a key twice will result
     * in the last value being used for a given header.
     *
     * @param key the header name (see "HttpHeaders.Names" for standard names).
     * @param value the header value (see "HttpHeaders.Values" for standard values).
     */
    public AsyncRestBuilder withHeader(String key, Object value) {
        this.headers.put(key, value);
        return this;
    }

    /**
     * Sets the body for the request without assuming a Content-Type or Accept header.
     * Note that you should avoid calling this for HTTP methods where it makes no sense
     * (eg. GET, DELETE), as it won't be ignored for these types of requests.
     *
     * @param body the raw body value to use, as a String.
     */
    public AsyncRestBuilder bodyRaw(String body) {
        this.body = body;
        return this;
    }

    /**
     * Sets the "Content-Type" standard header's value. This is a convenience
     * method equivalent to calling
     * {@link #withHeader(String, Object) withHeader("Content-Type", type)}.
     *
     * @param type the "Content-Type" to use.
     */
    public AsyncRestBuilder contentType(String type) {
        return withHeader(HttpHeaders.Names.CONTENT_TYPE, type);
    }

    /**
     * Sets the "Accept" standard header's value. This is a convenience
     * method equivalent to calling {@link #withHeader(String, Object) withHeader("Accept", type)}.
     *
     * @param type the "Accept" type to use.
     */
    public AsyncRestBuilder accept(String type) {
        return withHeader(HttpHeaders.Names.ACCEPT, type);
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
    public AsyncRestBuilder body(String jsonBody) {
        accept(HttpHeaders.Values.APPLICATION_JSON);
        contentType(HttpHeaders.Values.APPLICATION_JSON);
        bodyRaw(jsonBody);
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
    public AsyncRestBuilder body(JsonValue jsonBody) {
        return body(jsonBody.toString());
    }

    /**
     * Sets the body for the request to be an url-encoded form. This is equivalent to setting
     * the {@link #contentType(String) "Content-Type"} to <code>"application/x-www-form-urlencoded"</code>
     * and then setting the body via {@link #bodyRaw(String)}.
     *
     * @param form the {@link Form} builder object used to set form parameters.
     */
    public AsyncRestBuilder bodyForm(Form form) {
        contentType(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED);
        return bodyRaw(form.toUrlEncodedString());
    }

    //==== Getters ====

    /**
     * @return the {@link HttpMethod} used for this request.
     */
    public HttpMethod method() {
        return method;
    }

    /**
     * @return the full HTTP path (minus query parameters) used for this request.
     */
    public String path() {
        return this.path;
    }

    /**
     * @return the body used for this request.
     */
    public String body() {
        return this.body;
    }

    /**
     * @return a copy of the query parameters used for this request.
     */
    public Map<String, String> params() {
        return new LinkedHashMap<String, String>(params);
    }

    /**
     * @return a copy of the HTTP headers used for this request.
     */
    public Map<String, Object> headers() {
        return new LinkedHashMap<String, Object>(headers);
    }

    //==== RestApiRequest and execution ====

    /**
     * @return the {@link RestApiRequest} message sent through the {@link ClusterFacade}
     * when executing this request.
     */
    public RestApiRequest asRequest() {
        return new RestApiRequest(this.username, this.password,
                this.method, this.path, this.params, this.headers, this.body);
    }

    /**
     * Executes the API request in an asynchronous fashion.
     *
     * The return type is an {@link Observable} that will only emit the result of executing the request.
     * It is a cold Observable (and the request is only sent when it is subscribed to).
     *
     * @return an {@link Observable} of the result of the API call, which is a {@link RestApiResponse}.
     */
    public Observable<RestApiResponse> execute() {
        return deferAndWatch(new Func1<Subscriber, Observable<? extends RestApiResponse>>() {
            @Override
            public Observable<? extends RestApiResponse> call(Subscriber subscriber) {
                RestApiRequest apiRequest = asRequest();
                LOGGER.debug("Executing Cluster API request {} on {}", apiRequest.method(), apiRequest.pathWithParameters());
                apiRequest.subscriber(subscriber);
                return core.send(apiRequest);
            }
        });
    }

}
