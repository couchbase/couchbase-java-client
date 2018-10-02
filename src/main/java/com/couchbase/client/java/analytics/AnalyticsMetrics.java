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

import java.io.Serializable;

/**
 * Represents Analytics Metrics as part of the response.
 *
 * @author Michael Nitschinger
 * @since 2.4.3
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public class AnalyticsMetrics implements Serializable {

    private static final long serialVersionUID = -1955101433651194843L;

    /**
     * The empty metrics object. All numerical values will be 0 and human-readable times
     * will be {@link #NO_TIME}.
     */
    public static final AnalyticsMetrics EMPTY_METRICS = new AnalyticsMetrics();

    /**
     * Human-readable representation of the absence of duration, as "0s".
     */
    public static final String NO_TIME = "0s";

    private final JsonObject rawMetrics;

    private final int resultCount;
    private final int errorCount;
    private final int warningCount;
    private final int mutationCount;
    private final int sortCount;
    private final long processedObjects;
    private final long resultSize;
    private final String elapsedTime;
    private final String executionTime;

    private AnalyticsMetrics() {
        this(JsonObject.empty());
    }

    public AnalyticsMetrics(JsonObject rawMetrics) {
        this.rawMetrics = rawMetrics;

        if (rawMetrics.getString("elapsedTime") == null) {
            this.elapsedTime = NO_TIME;
        } else {
            this.elapsedTime = rawMetrics.getString("elapsedTime");
        }

        if (rawMetrics.getString("executionTime") == null) {
            this.executionTime = NO_TIME;
        } else {
            this.executionTime = rawMetrics.getString("executionTime");
        }

        this.resultCount  = parseBestEffortToInt(rawMetrics, "resultCount");
        this.errorCount = parseBestEffortToInt(rawMetrics, "errorCount");
        this.warningCount = parseBestEffortToInt(rawMetrics, "warningCount");
        this.mutationCount = parseBestEffortToInt(rawMetrics, "mutationCount");
        this.sortCount = parseBestEffortToInt(rawMetrics, "sortCount");
        this.resultSize = parseBestEffortToLong(rawMetrics, "resultSize");
        this.processedObjects = parseBestEffortToLong(rawMetrics, "processedObjects");
    }

    private int parseBestEffortToInt(JsonObject input, String fieldname) {
        Object value = input.get(fieldname);
        if (value == null) {
            return 0;
        }

        if (value instanceof Integer) {
            return (Integer) value;
        } else {
            return Integer.parseInt(value.toString());
        }
    }

    private long parseBestEffortToLong(JsonObject input, String fieldname) {
        Object value = input.get(fieldname);
        if (value == null) {
            return 0;
        }

        if (value instanceof Long) {
            return (Long) value;
        } else {
            return Long.parseLong(value.toString());
        }
    }


    /**
     * @return The total time taken for the request, that is the time from when the
     * request was received until the results were returned, in a human-readable
     * format (eg. 123.45ms for a little over 123 milliseconds).
     */
    public String elapsedTime() {
        return elapsedTime;
    }

    /**
     * @return The time taken for the execution of the request, that is the time from
     * when query execution started until the results were returned, in a human-readable
     * format (eg. 123.45ms for a little over 123 milliseconds).
     */
    public String executionTime() {
        return executionTime;
    }

    /**
     * @return the total number of results selected by the engine before restriction
     * through LIMIT clause.
     */
    public int sortCount() {
        return sortCount;
    }

    /**
     * @return The total number of objects in the results.
     */
    public int resultCount() {
        return resultCount;
    }

    /**
     * @return The total number of bytes in the results.
     */
    public long resultSize() {
        return resultSize;
    }

    /**
     * @return The number of mutations that were made during the request.
     */
    public int mutationCount() {
        return mutationCount;
    }
    /**
     * @return The number of errors that occurred during the request.
     */
    public int errorCount() {
        return errorCount;
    }

    /**
     * @return The number of warnings that occurred during the request.
     */
    public int warningCount() {
        return warningCount;
    }

    /**
     * @return The number of processed objects for the request.
     */
    public long processedObjects() {
        return processedObjects;
    }

    /**
     * Exposes the underlying raw form of the metrics, as a {@link JsonObject}.
     *
     * Note that values exposed as methods are cached at instantiation, so this
     * object is not backed by the returned JsonObject.
     *
     * @return the underlying raw form of the metrics.
     */
    public JsonObject asJsonObject() {
        return rawMetrics;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AnalyticsMetrics{");
        sb.append("resultCount=").append(resultCount);
        sb.append(", errorCount=").append(errorCount);
        sb.append(", warningCount=").append(warningCount);
        sb.append(", mutationCount=").append(mutationCount);
        sb.append(", sortCount=").append(sortCount);
        sb.append(", resultSize=").append(resultSize);
        sb.append(", processedObjects=").append(processedObjects);
        sb.append(", elapsedTime='").append(elapsedTime).append('\'');
        sb.append(", executionTime='").append(executionTime).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
