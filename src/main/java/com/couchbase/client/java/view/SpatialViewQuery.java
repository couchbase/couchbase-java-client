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

import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;

import java.io.Serializable;
import java.net.URLEncoder;
import java.util.Arrays;

/**
 * Fluent DSL for a Spatial View Query.
 *
 * @author Michael Nitschinger
 * @since 2.1.0
 */
public class SpatialViewQuery implements Serializable {

    private static final long serialVersionUID = -2743654475987033659L;

    private static final int PARAM_LIMIT_OFFSET = 0;
    private static final int PARAM_SKIP_OFFSET = 2;
    private static final int PARAM_STALE_OFFSET = 4;
    private static final int PARAM_DEBUG_OFFSET = 6;
    private static final int PARAM_START_RANGE_OFFSET = 8;
    private static final int PARAM_END_RANGE_OFFSET = 10;
    private static final int PARAM_ONERROR_OFFSET = 12;

    /**
     * Number of supported possible params for a query.
     */
    private static final int NUM_PARAMS = 7;

    /**
     * Contains all stored params.
     */
    private final String[] params;

    private final String design;
    private final String view;

    private boolean development;
    private boolean includeDocs;
    private Class<? extends Document<?>> includeDocsTarget;

    private SpatialViewQuery(String design, String view) {
        this.design = design;
        this.view = view;
        params = new String[NUM_PARAMS * 2];
        includeDocs = false;
        includeDocsTarget = null;
    }

    /**
     * Creates an new {@link SpatialViewQuery}.
     *
     * @param design the name of the design document.
     * @param view the name of the view.
     * @return a {@link SpatialViewQuery} DSL.
     */
    public static SpatialViewQuery from(String design, String view) {
        return new SpatialViewQuery(design, view);
    }

    public SpatialViewQuery development() {
        return development(true);
    }

    public SpatialViewQuery development(boolean development) {
        this.development = development;
        return this;
    }

    /**
     * Proactively load the full document for the row returned.
     *
     * This only works if reduce is false, since with reduce the original document ID is not included anymore.
     * @return the {@link SpatialViewQuery} DSL.
     */
    public SpatialViewQuery includeDocs() {
        return includeDocs(true, JsonDocument.class);
    }

    /**
     * Proactively load the full document for the row returned.
     *
     * This only works if reduce is false, since with reduce the original document ID is not included anymore.
     * @param target the custom document type target.
     * @return the {@link SpatialViewQuery} DSL.
     */
    public SpatialViewQuery includeDocs(Class<? extends Document<?>> target) {
        return includeDocs(true, target);
    }

    /**
     * Proactively load the full document for the row returned.
     *
     * This only works if reduce is false, since with reduce the original document ID is not included anymore.
     * @param includeDocs if it should be enabled or not.
     * @return the {@link SpatialViewQuery} DSL.
     */
    public SpatialViewQuery includeDocs(boolean includeDocs) {
        return includeDocs(includeDocs, JsonDocument.class);
    }

    /**
     * Proactively load the full document for the row returned.
     *
     * This only works if reduce is false, since with reduce the original document ID is not included anymore.
     * @param includeDocs if it should be enabled or not.
     * @param target the custom document type target.
     * @return the {@link SpatialViewQuery} DSL.
     */
    public SpatialViewQuery includeDocs(boolean includeDocs, Class<? extends Document<?>> target) {
        this.includeDocs = includeDocs;
        this.includeDocsTarget = target;
        return this;
    }

    /**
     * Limit the number of the returned documents to the specified number.
     *
     * @param limit the number of documents to return.
     * @return the {@link SpatialViewQuery} object for proper chaining.
     */
    public SpatialViewQuery limit(final int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Limit must be >= 0.");
        }
        params[PARAM_LIMIT_OFFSET] = "limit";
        params[PARAM_LIMIT_OFFSET+1] = Integer.toString(limit);
        return this;
    }

    /**
     * Skip this number of records before starting to return the results.
     *
     * @param skip The number of records to skip.
     * @return the {@link SpatialViewQuery} object for proper chaining.
     */
    public SpatialViewQuery skip(final int skip) {
        if (skip < 0) {
            throw new IllegalArgumentException("Skip must be >= 0.");
        }
        params[PARAM_SKIP_OFFSET] = "skip";
        params[PARAM_SKIP_OFFSET+1] = Integer.toString(skip);
        return this;
    }

    /**
     * Allow the results from a stale view to be used.
     *
     * See the "Stale" enum for more information on the possible options. The
     * default setting is "update_after"!
     *
     * @param stale Which stale mode should be used.
     * @return the {@link SpatialViewQuery} object for proper chaining.
     */
    public SpatialViewQuery stale(final Stale stale) {
        params[PARAM_STALE_OFFSET] = "stale";
        params[PARAM_STALE_OFFSET+1] = stale.identifier();
        return this;
    }

    /**
     * Enabled debugging on view queries.
     *
     * @return the {@link SpatialViewQuery} object for proper chaining.
     */
    public SpatialViewQuery debug() {
        return debug(true);
    }

    public SpatialViewQuery debug(boolean debug) {
        params[PARAM_DEBUG_OFFSET] = "debug";
        params[PARAM_DEBUG_OFFSET+1] = Boolean.toString(debug);
        return this;
    }

    public SpatialViewQuery startRange(final JsonArray startRange) {
        params[PARAM_START_RANGE_OFFSET] = "start_range";
        params[PARAM_START_RANGE_OFFSET+1] = startRange.toString();
        return this;
    }

    public SpatialViewQuery endRange(final JsonArray endRange) {
        params[PARAM_END_RANGE_OFFSET] = "end_range";
        params[PARAM_END_RANGE_OFFSET+1] = endRange.toString();
        return this;
    }

    public SpatialViewQuery range(final JsonArray startRange, final JsonArray endRange) {
        startRange(startRange);
        endRange(endRange);
        return this;
    }

    /**
     * Sets the response in the event of an error.
     *
     * See the "OnError" enum for more details on the available options.
     *
     * @param onError The appropriate error handling type.
     * @return the {@link SpatialViewQuery} object for proper chaining.
     */
    public SpatialViewQuery onError(final OnError onError) {
        params[PARAM_ONERROR_OFFSET] = "on_error";
        params[PARAM_ONERROR_OFFSET+1] = onError.identifier();
        return this;
    }

    /**
     * Helper method to properly encode a string.
     *
     * This method can be overridden if a different encoding logic needs to be
     * used.
     *
     * @param source source string.
     * @return encoded target string.
     */
    protected String encode(final String source) {
        try {
            return URLEncoder.encode(source, "UTF-8");
        } catch(Exception ex) {
            throw new RuntimeException("Could not prepare view argument: " + ex);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean firstParam = true;
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                i++;
                continue;
            }

            boolean even = i % 2 == 0;
            if (even) {
                if (!firstParam) {
                    sb.append("&");
                }
            }
            sb.append(params[i]);
            firstParam = false;
            if (even) {
                sb.append('=');
            }
        }
        return sb.toString();
    }

    public String getDesign() {
        return design;
    }

    public String getView() {
        return view;
    }

    public boolean isDevelopment() {
        return development;
    }

    public boolean isIncludeDocs() {
        return includeDocs;
    }

    public Class<? extends Document<?>> includeDocsTarget() {
        return includeDocsTarget;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpatialViewQuery query = (SpatialViewQuery) o;

        if (development != query.development) return false;
        if (design != null ? !design.equals(query.design) : query.design != null) return false;
        if (!Arrays.equals(params, query.params)) return false;
        if (view != null ? !view.equals(query.view) : query.view != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = params != null ? Arrays.hashCode(params) : 0;
        result = 31 * result + (design != null ? design.hashCode() : 0);
        result = 31 * result + (view != null ? view.hashCode() : 0);
        result = 31 * result + (development ? 1 : 0);
        return result;
    }
}
