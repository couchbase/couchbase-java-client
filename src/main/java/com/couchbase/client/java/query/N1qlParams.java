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
package com.couchbase.client.java.query;

import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.java.MutationState;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.document.json.JsonValue;
import com.couchbase.client.java.query.dsl.functions.ObjectFunctions;
import com.couchbase.client.java.subdoc.DocumentFragment;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Parameter Object for {@link N1qlQuery queries} that allows to fluently set most of the N1QL query parameters:
 *  - server side timeout
 *  - client context ID
 *  - scan consistency (with associated scan vector and/or scan wait if relevant)
 *  - max parallelism
 *
 * Note that these are different from statement-related named parameters or positional parameters.
 *
 * @author Simon Baslé
 * @since 2.1
 */
public class N1qlParams implements Serializable {

    private static final long serialVersionUID = 8888370260267213830L;

    private String serverSideTimeout;
    private ScanConsistency consistency;
    private String scanWait;
    private String clientContextId;
    private Integer maxParallelism;
    private boolean disableMetrics;
    private MutationState mutationState;
    private Map<String, Object> rawParams;

    /**
     * If adhoc, the query should never be prepared.
     */
    private boolean adhoc;

    private N1qlParams() {
        adhoc = true;
        disableMetrics = false;
    }

    /**
     * Modifies the given N1QL query (as a {@link JsonObject}) to reflect these {@link N1qlParams}.
     * @param queryJson the N1QL query
     */
    public void injectParams(JsonObject queryJson) {
        if (this.serverSideTimeout != null) {
            queryJson.put("timeout", this.serverSideTimeout);
        }
        if (this.consistency != null) {
            queryJson.put("scan_consistency", this.consistency.n1ql());
        }
        if (this.scanWait != null
                && (ScanConsistency.REQUEST_PLUS == this.consistency
                || ScanConsistency.STATEMENT_PLUS == this.consistency)) {
            queryJson.put("scan_wait", this.scanWait);
        }
        if (this.clientContextId != null) {
            queryJson.put("client_context_id", this.clientContextId);
        }
        if (this.maxParallelism != null) {
            queryJson.put("max_parallelism", this.maxParallelism.toString());
        }
        if (this.disableMetrics) {
            queryJson.put("metrics", false);
        }

        if (this.mutationState != null) {
            if (this.consistency != null) {
                throw new IllegalArgumentException("`consistency(...)` cannot be used "
                    + "together with `consistentWith(...)`");
            }
            queryJson.put("scan_vectors", mutationState.export());
            queryJson.put("scan_consistency", "at_plus");
        }

        if (this.rawParams != null) {
            for (Map.Entry<String, Object> entry : rawParams.entrySet()) {
                queryJson.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static String durationToN1qlFormat(long duration, TimeUnit unit) {
        switch (unit) {
            case NANOSECONDS:
                return duration + "ns";
            case MICROSECONDS:
                return duration + "us";
            case MILLISECONDS:
                return duration + "ms";
            case SECONDS:
                return duration + "s";
            case MINUTES:
                return duration + "m";
            case HOURS:
                return duration + "h";
            case DAYS:
            default:
                return unit.toHours(duration) + "h";
        }
    }

    /**
     * Start building a {@link N1qlParams}, allowing to customize an N1QL request.
     *
     * @return a new {@link N1qlParams}
     */
    public static N1qlParams build() {
        return new N1qlParams();
    }

    /**
     * Sets a maximum timeout for processing on the server side.
     *
     * @param timeout the duration of the timeout.
     * @param unit the unit of the timeout, from nanoseconds to hours.
     * @return this {@link N1qlParams} for chaining.
     */
    public N1qlParams serverSideTimeout(long timeout, TimeUnit unit) {
        this.serverSideTimeout = durationToN1qlFormat(timeout, unit);
        return this;
    }

    /**
     * Adds a client context ID to the request, that will be sent back in the response, allowing clients
     * to meaningfully trace requests/responses when many are exchanged.
     *
     * @param clientContextId the client context ID (null to send none)
     * @return this {@link N1qlParams} for chaining.
     */
    public N1qlParams withContextId(String clientContextId) {
        this.clientContextId = clientContextId;
        return this;
    }

    /**
     * If set to true (false being the default), the metrics object will not be returned from N1QL and
     * as a result be more efficient. Note that if metrics are disabled you are loosing information
     * to diagnose problems - so use with care!
     *
     * @param disableMetrics true if disabled, false otherwise (false = default).
     * @return this {@link N1qlParams} for chaining.
     */
    public N1qlParams disableMetrics(boolean disableMetrics) {
        this.disableMetrics = disableMetrics;
        return this;
    }

    /**
     * Sets scan consistency.
     *
     * Note that {@link ScanConsistency#NOT_BOUNDED NOT_BOUNDED} will unset the {@link #scanWait} if it was set.
     *
     * @return this {@link N1qlParams} for chaining.
     */
    public N1qlParams consistency(ScanConsistency consistency) {
        this.consistency = consistency;
        if (consistency == ScanConsistency.NOT_BOUNDED) {
            this.scanWait = null;
        }
        return this;
    }

    /**
     * Sets the {@link Document}s resulting of a mutation this query should be consistent with.
     *
     * @param documents the documents returned from a mutation.
     * @return this {@link N1qlParams} for chaining.
     */
    @InterfaceStability.Experimental
    public N1qlParams consistentWith(Document... documents) {
        return consistentWith(MutationState.from(documents));
    }

    /**
     * Sets the {@link DocumentFragment}s resulting of a mutation this query should be consistent with.
     *
     * @param fragments the fragments returned from a mutation.
     * @return this {@link N1qlParams} for chaining.
     */
    @InterfaceStability.Experimental
    public N1qlParams consistentWith(DocumentFragment... fragments) {
        return consistentWith(MutationState.from(fragments));
    }

    /**
     * Sets the {@link MutationState} this query should be consistent with.
     *
     * @param mutationState the mutation state which accumulates tokens from one or more mutation results.
     * @return this {@link N1qlParams} for chaining.
     */
    @InterfaceStability.Experimental
    public N1qlParams consistentWith(MutationState mutationState) {
        this.mutationState = mutationState;
        return this;
    }

    /**
     * If the {@link ScanConsistency#NOT_BOUNDED NOT_BOUNDED scan consistency} has been chosen, does nothing.
     *
     * Otherwise, sets the maximum time the client is willing to wait for an index to catch up to the
     * vector timestamp in the request.
     *
     * @param wait the duration.
     * @param unit the unit for the duration.
     * @return this {@link N1qlParams} for chaining.
     */
    public N1qlParams scanWait(long wait, TimeUnit unit) {
        if (this.consistency == ScanConsistency.NOT_BOUNDED) {
            this.scanWait = null;
        } else {
            this.scanWait = durationToN1qlFormat(wait, unit);
        }
        return this;
    }

    /**
     * Allows to override the default maximum parallelism for the query execution on the server side.
     *
     * @param maxParallelism the maximum parallelism for this query, 0 or negative values disable it.
     * @return this {@link N1qlParams} for chaining.
     */
    public N1qlParams maxParallelism(int maxParallelism) {
        this.maxParallelism = maxParallelism;
        return this;
    }


    /**
     * Allows to specify if this query is adhoc or not.
     *
     * If it is not adhoc (so performed often), the client will try to perform optimizations
     * transparently based on the server capabilities, like preparing the statement and
     * then executing a query plan instead of the raw query.
     *
     * @param adhoc if the query is adhoc, default is true (plain execution).
     * @return this {@link N1qlParams} for chaining.
     */
    public N1qlParams adhoc(boolean adhoc) {
        this.adhoc = adhoc;
        return this;
    }

    /**
     * Allows to specify an arbitrary, raw N1QL param.
     *
     * Use with care and only provide options that are supported by the server and are not exposed as part of the
     * overall stable API in the {@link N1qlParams} class.
     *
     * @param name the name of the property.
     * @param value the value of the property, only JSON value types are supported.
     * @return this {@link N1qlParams} for chaining.
     */
    @InterfaceStability.Uncommitted
    public N1qlParams rawParam(String name, Object value) {
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
     * Helper method to check if a custom server side timeout has been applied on the params.
     *
     * @return true if it has, false otherwise.
     */
    public boolean hasServerSideTimeout() {
        return serverSideTimeout != null;
    }

    /**
     * True if this query is adhoc, false otherwise.
     *
     * @return true if adhoc false otherwise.
     */
    public boolean isAdhoc() {
        return adhoc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        N1qlParams that = (N1qlParams) o;

        if (disableMetrics != that.disableMetrics) return false;
        if (adhoc != that.adhoc) return false;
        if (serverSideTimeout != null ? !serverSideTimeout.equals(that.serverSideTimeout) : that.serverSideTimeout != null)
            return false;
        if (consistency != that.consistency) return false;
        if (scanWait != null ? !scanWait.equals(that.scanWait) : that.scanWait != null) return false;
        if (clientContextId != null ? !clientContextId.equals(that.clientContextId) : that.clientContextId != null)
            return false;
        if (maxParallelism != null ? !maxParallelism.equals(that.maxParallelism) : that.maxParallelism != null)
            return false;
        if (mutationState != null ? !mutationState.equals(that.mutationState) : that.mutationState != null)
            return false;
        return rawParams != null ? rawParams.equals(that.rawParams) : that.rawParams == null;

    }

    @Override
    public int hashCode() {
        int result = serverSideTimeout != null ? serverSideTimeout.hashCode() : 0;
        result = 31 * result + (consistency != null ? consistency.hashCode() : 0);
        result = 31 * result + (scanWait != null ? scanWait.hashCode() : 0);
        result = 31 * result + (clientContextId != null ? clientContextId.hashCode() : 0);
        result = 31 * result + (maxParallelism != null ? maxParallelism.hashCode() : 0);
        result = 31 * result + (disableMetrics ? 1 : 0);
        result = 31 * result + (mutationState != null ? mutationState.hashCode() : 0);
        result = 31 * result + (rawParams != null ? rawParams.hashCode() : 0);
        result = 31 * result + (adhoc ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("N1qlParams{");
        sb.append("serverSideTimeout='").append(serverSideTimeout).append('\'');
        sb.append(", consistency=").append(consistency);
        sb.append(", scanWait='").append(scanWait).append('\'');
        sb.append(", clientContextId='").append(clientContextId).append('\'');
        sb.append(", maxParallelism=").append(maxParallelism);
        sb.append(", adhoc=").append(adhoc);
        sb.append(", disableMetrics=").append(disableMetrics);
        sb.append(", rawParams=").append(rawParams);
        sb.append('}');
        return sb.toString();
    }
}
