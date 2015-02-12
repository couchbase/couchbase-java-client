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
    private static final int PARAM_KEYS_OFFSET = 28;
    private static final int PARAM_KEY_OFFSET = 30;

    /**
     * Number of supported possible params for a query.
     */
    private static final int NUM_PARAMS = 16;

    /**
     * Contains all stored params.
     */
    private final String[] params;

    private final String design;
    private final String view;

    private boolean development;

    private ViewQuery(String design, String view) {
        this.design = design;
        this.view = view;
        params = new String[NUM_PARAMS * 2];
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
        params[PARAM_KEYS_OFFSET] = "keys";
        params[PARAM_KEYS_OFFSET+1] = encode(keys.toString());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ViewQuery viewQuery = (ViewQuery) o;

        if (development != viewQuery.development) return false;
        if (design != null ? !design.equals(viewQuery.design) : viewQuery.design != null) return false;
        if (!Arrays.equals(params, viewQuery.params)) return false;
        if (view != null ? !view.equals(viewQuery.view) : viewQuery.view != null) return false;

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
