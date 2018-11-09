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
import com.couchbase.client.java.document.json.JsonObject;

import java.io.Serializable;
import java.net.URLEncoder;
import java.util.Arrays;

/**
 * Fluent DSL for a View Query.
 *
 * @author Michael Nitschinger
 * @since 2.0.0
 */
public class ViewQuery implements Serializable {

    private static final long serialVersionUID = -9127974725934261293L;

    private static final int PARAM_REDUCE_OFFSET = 0;
    private static final int PARAM_LIMIT_OFFSET = 2;
    private static final int PARAM_SKIP_OFFSET = 4;
    private static final int PARAM_STALE_OFFSET = 6;
    private static final int PARAM_GROUPLEVEL_OFFSET = 8;
    private static final int PARAM_GROUP_OFFSET = 10;
    private static final int PARAM_ONERROR_OFFSET = 12;
    private static final int PARAM_DEBUG_OFFSET = 14;
    private static final int PARAM_DESCENDING_OFFSET = 16;
    private static final int PARAM_INCLUSIVEEND_OFFSET = 18;
    private static final int PARAM_STARTKEY_OFFSET = 20;
    private static final int PARAM_STARTKEYDOCID_OFFSET = 22;
    private static final int PARAM_ENDKEY_OFFSET = 24;
    private static final int PARAM_ENDKEYDOCID_OFFSET = 26;
    private static final int PARAM_KEY_OFFSET = 28;

    /**
     * Number of supported possible params for a query.
     */
    private static final int NUM_PARAMS = 15;

    /**
     * Contains all stored params.
     */
    private final String[] params;

    private final String design;
    private final String view;

    private boolean development;
    private boolean includeDocs;
    private boolean retainOrder;
    private Class<? extends Document<?>> includeDocsTarget;
    private String keysJson;

    private ViewQuery(String design, String view) {
        this.design = design;
        this.view = view;
        params = new String[NUM_PARAMS * 2];
        includeDocs = false;
        retainOrder = false;
        includeDocsTarget = null;
    }

    /**
     * Creates an new {@link ViewQuery}.
     *
     * @param design the name of the design document.
     * @param view the name of the view.
     * @return a {@link ViewQuery} DSL.
     */
    public static ViewQuery from(String design, String view) {
        return new ViewQuery(design, view);
    }

    public ViewQuery development() {
        return development(true);
    }

    public ViewQuery development(boolean development) {
        this.development = development;
        return this;
    }

    /**
     * Proactively load the full document for the row returned, while strictly retaining view row order.
     *
     * This only works if reduce is false, since with reduce the original document ID is not included anymore.
     * @return the {@link ViewQuery} DSL.
     */
    public ViewQuery includeDocsOrdered() {
        return includeDocsOrdered(true, JsonDocument.class);
    }

    /**
     * Proactively load the full document for the row returned, while strictly retaining view row order.
     *
     * This only works if reduce is false, since with reduce the original document ID is not included anymore.
     * @param target the custom document type target.
     * @return the {@link ViewQuery} DSL.
     */
    public ViewQuery includeDocsOrdered(Class<? extends Document<?>> target) {
        return includeDocsOrdered(true, target);
    }

    /**
     * Proactively load the full document for the row returned, while strictly retaining view row order.
     *
     * This only works if reduce is false, since with reduce the original document ID is not included anymore.
     * @param includeDocs if it should be enabled or not.
     * @return the {@link ViewQuery} DSL.
     */
    public ViewQuery includeDocsOrdered(boolean includeDocs) {
        return includeDocsOrdered(includeDocs, JsonDocument.class);
    }

    /**
     * Proactively load the full document for the row returned, while strictly retaining view row order.
     *
     * This only works if reduce is false, since with reduce the original document ID is not included anymore.
     * @param includeDocs if it should be enabled or not.
     * @param target the custom document type target.
     * @return the {@link ViewQuery} DSL.
     */
    public ViewQuery includeDocsOrdered(boolean includeDocs, Class<? extends Document<?>> target) {
        this.includeDocs = includeDocs;
        this.retainOrder = includeDocs; //deactivate if includeDocs is deactivated
        this.includeDocsTarget = target;
        return this;
    }

    /**
     * Proactively load the full document for the row returned.
     *
     * This only works if reduce is false, since with reduce the original document ID is not included anymore.
     *
     * The order could be changed if one of the document takes longer to load than the others
     * (see {@link #includeDocsOrdered()} )} if you want to enforce the row order).
     *
     * @return the {@link ViewQuery} DSL.
     */
    public ViewQuery includeDocs() {
        return includeDocs(true, JsonDocument.class);
    }

    /**
     * Proactively load the full document for the row returned.
     *
     * This only works if reduce is false, since with reduce the original document ID is not included anymore.
     *
     * The order could be changed if one of the document takes longer to load than the others
     * (see {@link #includeDocsOrdered(Class)} )} if you want to enforce the row order).
     *
     * @param target the custom document type target.
     * @return the {@link ViewQuery} DSL.
     */
    public ViewQuery includeDocs(Class<? extends Document<?>> target) {
        return includeDocs(true, target);
    }

    /**
     * Proactively load the full document for the row returned.
     *
     * This only works if reduce is false, since with reduce the original document ID is not included anymore.
     *
     * The order could be changed if one of the document takes longer to load than the others
     * (see {@link #includeDocsOrdered(boolean)} if you want to enforce the row order).
     *
     * @param includeDocs if it should be enabled or not.
     * @return the {@link ViewQuery} DSL.
     */
    public ViewQuery includeDocs(boolean includeDocs) {
        return includeDocs(includeDocs, JsonDocument.class);
    }

    /**
     * Proactively load the full document for the row returned.
     *
     * This only works if reduce is false, since with reduce the original document ID is not included anymore.
     *
     * The order could be changed if one of the document takes longer to load than the others
     * (see {@link #includeDocsOrdered(boolean, Class)} if you want to enforce the row order).
     *
     * @param includeDocs if it should be enabled or not.
     * @param target the custom document type target.
     * @return the {@link ViewQuery} DSL.
     */
    public ViewQuery includeDocs(boolean includeDocs, Class<? extends Document<?>> target) {
        this.includeDocs = includeDocs;
        this.retainOrder = false;
        this.includeDocsTarget = target;
        return this;
    }

    /**
     * Explicitly enable/disable the reduce function on the query.
     *
     * @param reduce if reduce should be enabled or not.
     * @return the {@link ViewQuery} object for proper chaining.
     */
    public ViewQuery reduce(final boolean reduce) {
        params[PARAM_REDUCE_OFFSET] = "reduce";
        params[PARAM_REDUCE_OFFSET+1] = Boolean.toString(reduce);
        return this;
    }

    public ViewQuery reduce() {
        return reduce(true);
    }


    /**
     * Limit the number of the returned documents to the specified number.
     *
     * @param limit the number of documents to return.
     * @return the {@link ViewQuery} object for proper chaining.
     */
    public ViewQuery limit(final int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Limit must be >= 0.");
        }
        params[PARAM_LIMIT_OFFSET] = "limit";
        params[PARAM_LIMIT_OFFSET+1] = Integer.toString(limit);
        return this;
    }

    /**
     * Group the results using the reduce function to a group or single row.
     *
     * Important: this setter and {@link #groupLevel(int)} should not be used
     * together in the same {@link ViewQuery}. It is sufficient to only set the
     * grouping level only and use this setter in cases where you always want the
     * highest group level implictly.
     *
     * @return the {@link ViewQuery} object for proper chaining.
     */
    public ViewQuery group() {
        return group(true);
    }

    public ViewQuery group(boolean group) {
        params[PARAM_GROUP_OFFSET] = "group";
        params[PARAM_GROUP_OFFSET+1] = Boolean.toString(group);
        return this;
    }

    /**
     * Specify the group level to be used.
     *
     * Important: {@link #group()} and this setter should not be used
     * together in the same {@link ViewQuery}. It is sufficient to only use this
     * setter and use {@link #group()} in cases where you always want
     * the highest group level implictly.
     *
     * @param grouplevel How deep the grouping level should be.
     * @return the {@link ViewQuery} object for proper chaining.
     */
    public ViewQuery groupLevel(final int grouplevel) {
        params[PARAM_GROUPLEVEL_OFFSET] = "group_level";
        params[PARAM_GROUPLEVEL_OFFSET+1] = Integer.toString(grouplevel);
        return this;
    }


    /**
     * Specifies whether the specified end key should be included in the result.
     *
     * @return the {@link ViewQuery} object for proper chaining.
     */
    public ViewQuery inclusiveEnd() {
        return inclusiveEnd(true);
    }

    public ViewQuery inclusiveEnd(boolean inclusive) {
        params[PARAM_INCLUSIVEEND_OFFSET] = "inclusive_end";
        params[PARAM_INCLUSIVEEND_OFFSET+1] = Boolean.toString(inclusive);
        return this;
    }


    /**
     * Skip this number of records before starting to return the results.
     *
     * @param skip The number of records to skip.
     * @return the {@link ViewQuery} object for proper chaining.
     */
    public ViewQuery skip(final int skip) {
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
     * @return the {@link ViewQuery} object for proper chaining.
     */
    public ViewQuery stale(final Stale stale) {
        params[PARAM_STALE_OFFSET] = "stale";
        params[PARAM_STALE_OFFSET+1] = stale.identifier();
        return this;
    }

    /**
     * Sets the response in the event of an error.
     *
     * See the "OnError" enum for more details on the available options.
     *
     * @param onError The appropriate error handling type.
     * @return the {@link ViewQuery} object for proper chaining.
     */
    public ViewQuery onError(final OnError onError) {
        params[PARAM_ONERROR_OFFSET] = "on_error";
        params[PARAM_ONERROR_OFFSET+1] = onError.identifier();
        return this;
    }

    /**
     * Enabled debugging on view queries.
     *
     * @return the {@link ViewQuery} object for proper chaining.
     */
    public ViewQuery debug() {
        return debug(true);
    }

    public ViewQuery debug(boolean debug) {
        params[PARAM_DEBUG_OFFSET] = "debug";
        params[PARAM_DEBUG_OFFSET+1] = Boolean.toString(debug);
        return this;
    }

    /**
     * Return the documents in descending by key order.
     *
     * @return the {@link ViewQuery} object for proper chaining.
     */
    public ViewQuery descending() {
        return descending(true);
    }

    public ViewQuery descending(boolean desc) {
        params[PARAM_DESCENDING_OFFSET] = "descending";
        params[PARAM_DESCENDING_OFFSET+1] = Boolean.toString(desc);
        return this;
    }


    public ViewQuery key(String key) {
        params[PARAM_KEY_OFFSET] = "key";
        params[PARAM_KEY_OFFSET+1] = encode("\"" + key + "\"");
        return this;
    }

    public ViewQuery key(int key) {
        params[PARAM_KEY_OFFSET] = "key";
        params[PARAM_KEY_OFFSET+1] = Integer.toString(key);
        return this;
    }

    public ViewQuery key(long key) {
        params[PARAM_KEY_OFFSET] = "key";
        params[PARAM_KEY_OFFSET+1] = Long.toString(key);
        return this;
    }

    public ViewQuery key(double key) {
        params[PARAM_KEY_OFFSET] = "key";
        params[PARAM_KEY_OFFSET+1] = Double.toString(key);
        return this;
    }


    public ViewQuery key(boolean key) {
        params[PARAM_KEY_OFFSET] = "key";
        params[PARAM_KEY_OFFSET+1] = Boolean.toString(key);
        return this;
    }

    public ViewQuery key(JsonObject key) {
        params[PARAM_KEY_OFFSET] = "key";
        params[PARAM_KEY_OFFSET+1] = encode(key.toString());
        return this;
    }

    public ViewQuery key(JsonArray key) {
        params[PARAM_KEY_OFFSET] = "key";
        params[PARAM_KEY_OFFSET+1] = encode(key.toString());
        return this;
    }

    public ViewQuery keys(JsonArray keys) {
        this.keysJson = keys.toString();
        return this;
    }

    public ViewQuery startKeyDocId(String id) {
        params[PARAM_STARTKEYDOCID_OFFSET] = "startkey_docid";
        params[PARAM_STARTKEYDOCID_OFFSET+1] = encode(id);
        return this;
    }

    public ViewQuery endKeyDocId(String id) {
        params[PARAM_ENDKEYDOCID_OFFSET] = "endkey_docid";
        params[PARAM_ENDKEYDOCID_OFFSET+1] = encode(id);
        return this;
    }

    public ViewQuery endKey(String key) {
        params[PARAM_ENDKEY_OFFSET] = "endkey";
        params[PARAM_ENDKEY_OFFSET+1] = encode("\"" + key + "\"");
        return this;
    }

    public ViewQuery endKey(int key) {
        params[PARAM_ENDKEY_OFFSET] = "endkey";
        params[PARAM_ENDKEY_OFFSET+1] = Integer.toString(key);
        return this;
    }

    public ViewQuery endKey(long key) {
        params[PARAM_ENDKEY_OFFSET] = "endkey";
        params[PARAM_ENDKEY_OFFSET+1] = Long.toString(key);
        return this;
    }

    public ViewQuery endKey(double key) {
        params[PARAM_ENDKEY_OFFSET] = "endkey";
        params[PARAM_ENDKEY_OFFSET+1] = Double.toString(key);
        return this;
    }


    public ViewQuery endKey(boolean key) {
        params[PARAM_ENDKEY_OFFSET] = "endkey";
        params[PARAM_ENDKEY_OFFSET+1] = Boolean.toString(key);
        return this;
    }

    public ViewQuery endKey(JsonObject key) {
        params[PARAM_ENDKEY_OFFSET] = "endkey";
        params[PARAM_ENDKEY_OFFSET+1] = encode(key.toString());
        return this;
    }

    public ViewQuery endKey(JsonArray key) {
        params[PARAM_ENDKEY_OFFSET] = "endkey";
        params[PARAM_ENDKEY_OFFSET+1] = encode(key.toString());
        return this;
    }

    public ViewQuery startKey(String key) {
        params[PARAM_STARTKEY_OFFSET] = "startkey";
        params[PARAM_STARTKEY_OFFSET+1] = encode("\"" + key + "\"");
        return this;
    }

    public ViewQuery startKey(int key) {
        params[PARAM_STARTKEY_OFFSET] = "startkey";
        params[PARAM_STARTKEY_OFFSET+1] = Integer.toString(key);
        return this;
    }

    public ViewQuery startKey(long key) {
        params[PARAM_STARTKEY_OFFSET] = "startkey";
        params[PARAM_STARTKEY_OFFSET+1] = Long.toString(key);
        return this;
    }

    public ViewQuery startKey(double key) {
        params[PARAM_STARTKEY_OFFSET] = "startkey";
        params[PARAM_STARTKEY_OFFSET+1] = Double.toString(key);
        return this;
    }


    public ViewQuery startKey(boolean key) {
        params[PARAM_STARTKEY_OFFSET] = "startkey";
        params[PARAM_STARTKEY_OFFSET+1] = Boolean.toString(key);
        return this;
    }

    public ViewQuery startKey(JsonObject key) {
        params[PARAM_STARTKEY_OFFSET] = "startkey";
        params[PARAM_STARTKEY_OFFSET+1] = encode(key.toString());
        return this;
    }

    public ViewQuery startKey(JsonArray key) {
        params[PARAM_STARTKEY_OFFSET] = "startkey";
        params[PARAM_STARTKEY_OFFSET+1] = encode(key.toString());
        return this;
    }

    /**
     * Helper method to properly encode a string.
     *
     * This method can be overridden if a different encoding logic needs to be
     * used. If so, note that {@link #keys(JsonArray) keys} is not encoded via
     * this method, but by the core.
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

    /**
     * Returns the query string for this ViewQuery, containing all the key/value pairs
     * for parameters that will be part of this ViewQuery's execution URL for the view service.
     */
    public String toQueryString() {
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

    /**
     * A string representation of this ViewQuery, suitable for logging and other human consumption.
     * If the {@link #keys(JsonArray)} parameter is too large, it is truncated in this dump.
     *
     * see the {@link #toQueryString()} for the parameter representation of the ViewQuery execution URL.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ViewQuery(").append(design).append('/').append(view).append("){");
        sb.append("params=\"").append(toQueryString()).append('"');
        if (isDevelopment()) {
            sb.append(", dev");
        }
        if (isIncludeDocs()) {
            sb.append(", includeDocs");
        }
        if (keysJson != null) {
            sb.append(", keys=\"");
            if (keysJson.length() < 140) {
                sb.append(keysJson).append('"');
            } else {
                sb.append(keysJson, 0, 140)
                    .append("...\"(")
                    .append(keysJson.length())
                    .append(" chars total)");
            }
        }
        sb.append('}');
        return sb.toString();
    }

    public String getDesign() {
        return design;
    }

    public String getView() {
        return view;
    }

    /**
     * @return the String JSON representation of the {@link #keys(JsonArray) keys} parameter.
     */
    public String getKeys() {
        return this.keysJson;
    }

    /**
     * @return true if row order, as returned by the view, should be kept while also {@link #includeDocs() including docs}.
     */
    public boolean isOrderRetained() {
        return this.retainOrder;
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

        ViewQuery viewQuery = (ViewQuery) o;

        if (development != viewQuery.development) return false;
        if (design != null ? !design.equals(viewQuery.design) : viewQuery.design != null) return false;
        if (!Arrays.equals(params, viewQuery.params)) return false;
        if (keysJson != null ? !keysJson.equals(viewQuery.keysJson) : viewQuery.keysJson != null) return false;
        if (view != null ? !view.equals(viewQuery.view) : viewQuery.view != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = params != null ? Arrays.hashCode(params) : 0;
        result = 31 * result + (design != null ? design.hashCode() : 0);
        result = 31 * result + (view != null ? view.hashCode() : 0);
        result = 31 * result + (development ? 1 : 0);
        result = 31 * result + (keysJson != null ? keysJson.hashCode() : 0);
        return result;
    }
}
