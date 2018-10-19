/*
 * Copyright (c) 2017 Couchbase, Inc.
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
package com.couchbase.client.java.analytics;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.document.json.JsonValue;
import com.couchbase.client.java.query.N1qlParams;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.couchbase.client.java.query.N1qlParams.durationToN1qlFormat;

/**
 * Parameters for Analytics Queries.
 *
 * @author Michael Nitschinger
 * @since 2.4.3
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public class AnalyticsParams implements Serializable {

    private static final long serialVersionUID = 8888370260267213836L;

    private String serverSideTimeout;
    private String clientContextId;
    private Map<String, Object> rawParams;
    private boolean pretty;

    /**
     * We are exposing this as a boolean, but internally the server
     * wants it as int to be forwards compatible (since at some
     * point we might want to expose this as a number to users).
     */
    private int priority;

    private boolean deferred;


    private AnalyticsParams() {
        pretty = false;
        priority = 0;
        deferred = false;
    }

    /**
     * Modifies the given Analytics query (as a {@link JsonObject}) to reflect these {@link N1qlParams}.
     * @param queryJson the Analytics query
     */
    public void injectParams(JsonObject queryJson) {
        if (this.serverSideTimeout != null) {
            queryJson.put("timeout", this.serverSideTimeout);
        }

        if (this.clientContextId != null) {
            queryJson.put("client_context_id", this.clientContextId);
        }

        if (pretty) {
            queryJson.put("pretty", true);
        }

        if (deferred) {
            queryJson.put("mode", "async");
        }

        if (this.rawParams != null) {
            for (Map.Entry<String, Object> entry : rawParams.entrySet()) {
                queryJson.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Adds a client context ID to the request, that will be sent back in the response, allowing clients
     * to meaningfully trace requests/responses when many are exchanged.
     *
     * @param clientContextId the client context ID (null to send none)
     * @return this {@link AnalyticsParams} for chaining.
     */
    public AnalyticsParams withContextId(String clientContextId) {
        this.clientContextId = clientContextId;
        return this;
    }

    /**
     * Sets a maximum timeout for processing on the server side.
     *
     * @param timeout the duration of the timeout.
     * @param unit the unit of the timeout, from nanoseconds to hours.
     * @return this {@link AnalyticsParams} for chaining.
     */
    public AnalyticsParams serverSideTimeout(long timeout, TimeUnit unit) {
        this.serverSideTimeout = durationToN1qlFormat(timeout, unit);
        return this;
    }

    /**
     * Allows to specify an arbitrary, raw Analytics param.
     *
     * Use with care and only provide options that are supported by the server and are not exposed as part of the
     * overall stable API in the {@link AnalyticsParams} class.
     *
     * @param name the name of the property.
     * @param value the value of the property, only JSON value types are supported.
     * @return this {@link AnalyticsParams} for chaining.
     */
    @InterfaceStability.Uncommitted
    public AnalyticsParams rawParam(String name, Object value) {
        if (this.rawParams == null) {
            this.rawParams = new HashMap<String, Object>();
        }

        if (!JsonValue.checkType(value)) {
            throw new IllegalArgumentException("Only JSON types are supported.");
        }

        rawParams.put(name, value);
        return this;
    }

    /**
     * If set to false, the server will be instructed to remove extra whitespace from the JSON response
     * in order to save bytes. In performance-critical environments as well as large responses this is
     * recommended in order to cut down on network traffic.
     *
     * Note that false is the default, since usually that's what is recommended.
     *
     * @param pretty if set to false, pretty responses are disabled.
     * @return this {@link AnalyticsParams} for chaining.
     */
    public AnalyticsParams pretty(boolean pretty) {
        this.pretty = pretty;
        return this;
    }

    /**
     * If this request should receive priority in server side scheduling.
     *
     * Default is false.
     *
     * @param priority true if it should, false otherwise.
     * @return this {@link AnalyticsParams} for chaining.
     */
    public AnalyticsParams priority(boolean priority) {
        return priority(priority ? -1 : 0);
    }

    /**
     * Sets the priority of this request.
     *
     * This method is private for now, but can be opened later once the
     * server supports different levels.
     *
     * @param priority the priority to use
     * @return this {@link AnalyticsParams} for chaining.
     */
    private AnalyticsParams priority(int priority) {
        this.priority = priority;
        return this;
    }

    @InterfaceAudience.Private
    public boolean deferred() {
        return deferred;
    }

  /**
     * Set to true for deferred query execution
     * (Translates to mode async for the server request)
     *
     * @param deferred true for deferred execution, false otherwise.
     * @return this {@link AnalyticsParams} for chaining.
     */
    public AnalyticsParams deferred(boolean deferred) {
        this.deferred = deferred;
        return this;
    }

    /**
     * Returns the set priority as an integer.
     *
     * This method is considered private API.
     *
     * @return the priority if set, 0 if not set.
     */
    @InterfaceAudience.Private
    public int priority() {
        return priority;
    }


    /**
     * Helper method to check if a custom server side timeout has been applied on the params.
     *
     * @return true if it has, false otherwise.
     */
    public boolean hasServerSideTimeout() {
        return serverSideTimeout != null;
    }

    /**
     * Helper method to check if a client context ID is set.
     */
    public String clientContextId() {
        return clientContextId;
    }

    /**
     * Start building a {@link AnalyticsParams}, allowing to customize an Analytics requests.
     *
     * @return a new {@link AnalyticsParams}
     */
    public static AnalyticsParams build() {
        return new AnalyticsParams();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnalyticsParams that = (AnalyticsParams) o;

        if (pretty != that.pretty) return false;
        if (priority != that.priority) return false;
        if (deferred != that.deferred) return false;
        if (serverSideTimeout != null ? !serverSideTimeout.equals(that.serverSideTimeout) : that.serverSideTimeout != null)
            return false;
        if (clientContextId != null ? !clientContextId.equals(that.clientContextId) : that.clientContextId != null)
            return false;
        return rawParams != null ? rawParams.equals(that.rawParams) : that.rawParams == null;
    }

    @Override
    public int hashCode() {
        int result = serverSideTimeout != null ? serverSideTimeout.hashCode() : 0;
        result = 31 * result + (clientContextId != null ? clientContextId.hashCode() : 0);
        result = 31 * result + (rawParams != null ? rawParams.hashCode() : 0);
        result = 31 * result + (pretty ? 1 : 0);
        result = 31 * result + priority;
        result = 31 * result + (deferred ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AnalyticsParams{" +
            "serverSideTimeout='" + serverSideTimeout + '\'' +
            ", clientContextId='" + clientContextId + '\'' +
            ", rawParams=" + rawParams +
            ", pretty=" + pretty +
            ", priority=" + priority +
            ", deferred=" + deferred +
            '}';
    }
}
