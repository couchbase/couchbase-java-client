/**
 * Copyright (C) 2014 Couchbase, Inc.
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
    private static final int PARAM_BBOX_OFFSET = 8;
    private static final int PARAM_ONERROR_OFFSET = 10;

    /**
     * Number of supported possible params for a query.
     */
    private static final int NUM_PARAMS = 6;

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
    /**
     * Create a box selection
     * @param left the left limit
     * @param bottom the bottom limit
     * @param right the right limit
     * @param top the top limit
     * @return the {@link SpatialViewQuery} object for proper chaining. 
     */
    public SpatialViewQuery bbox(double left, double bottom, double right, double top){
        params[PARAM_BBOX_OFFSET] = "bbox";
        params[PARAM_BBOX_OFFSET+1] = left+","+bottom+","+right+","+top;
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
