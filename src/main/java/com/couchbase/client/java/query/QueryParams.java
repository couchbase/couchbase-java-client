/**
 * Copyright (C) 2015 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */
package com.couchbase.client.java.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.document.json.JsonValue;
import com.couchbase.client.java.query.consistency.ScanConsistency;

/**
 * Parameter Object for {@link Query queries} that allows to fluently set most of the N1QL query parameters:
 *  - server side timeout
 *  - credentials
 *  - client context ID
 *  - scan consistency (with associated scan vector and/or scan wait if relevant)
 *
 * Note that these are different from statement-related named parameters or positional parameters.
 *
 * @author Simon Baslé
 * @since 2.1
 */
public class QueryParams {

    private String serverSideTimeout;
    private ScanConsistency consistency;
    private String scanWait;
    private String clientContextId;

    private QueryParams() { }

    /**
     * Modifies the given N1QL query (as a {@link JsonObject}) to reflect these {@link QueryParams}.
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
     * Start building a {@link QueryParams}, allowing to customize an N1QL request.
     *
     * @return a new {@link QueryParams}
     */
    public static QueryParams build() {
        return new QueryParams();
    }

    /**
     * Sets a maximum timeout for processing on the server side.
     *
     * @param timeout the duration of the timeout.
     * @param unit the unit of the timeout, from nanoseconds to hours.
     * @return this {@link QueryParams} for chaining.
     */
    public QueryParams serverSideTimeout(long timeout, TimeUnit unit) {
        this.serverSideTimeout = durationToN1qlFormat(timeout, unit);
        return this;
    }

    /**
     * Adds a client context ID to the request, that will be sent back in the response, allowing clients
     * to meaningfully trace requests/responses when many are exchanged.
     *
     * @param clientContextId the client context ID (null to send none)
     * @return this {@link QueryParams} for chaining.
     */
    public QueryParams withContextId(String clientContextId) {
        this.clientContextId = clientContextId;
        return this;
    }

    /**
     * Sets scan consistency.
     *
     * Note that {@link ScanConsistency#NOT_BOUNDED NOT_BOUNDED} will unset the {@link #scanWait} if it was set.
     *
     * @return this {@link QueryParams} for chaining.
     */
    public QueryParams consistency(ScanConsistency consistency) {
        this.consistency = consistency;
        if (consistency == ScanConsistency.NOT_BOUNDED) {
            this.scanWait = null;
        }
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
     * @return this {@link QueryParams} for chaining.
     */
    public QueryParams scanWait(long wait, TimeUnit unit) {
        if (this.consistency == ScanConsistency.NOT_BOUNDED) {
            this.scanWait = null;
        } else {
            this.scanWait = durationToN1qlFormat(wait, unit);
        }
        return this;
    }
}
