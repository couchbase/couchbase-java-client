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
package com.couchbase.client.java.fts;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.fts.facet.SearchFacet;
import com.couchbase.client.java.fts.result.SearchQueryResult;
import com.couchbase.client.java.fts.result.SearchQueryRow;

/**
 * The global parameters applied at top-level on a {@link SearchQuery FTS query}.
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class SearchParams {

    private Integer limit;
    private Integer skip;
    private Boolean explain;
    private HighlightStyle highlightStyle;
    private String[] highlightFields;
    private String[] fields;
    private Map<String, SearchFacet> facets;
    private Long serverSideTimeout;

    private SearchParams() {
        this.highlightFields = new String[0];
        this.fields = new String[0];
        this.facets = new HashMap<String, SearchFacet>();
    }

    /**
     * Creates an empty {@link SearchParams} instance that can be used as a builder.
     */
    public static SearchParams build() {
        return new SearchParams();
    }

    /**
     * Add a limit to the query on the number of hits it can return.
     *
     * @param limit the maximum number of hits to return.
     * @return this SearchParams for chaining.
     */
    public SearchParams limit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Set the number of hits to skip (eg. for pagination).
     *
     * @param skip the number of results to skip.
     * @return this SearchParams for chaining.
     */
    public SearchParams skip(int skip) {
        this.skip = skip;
        return this;
    }

    /**
     * Activates the explanation of each result hit in the response.
     *
     * @return this SearchParams for chaining.
     */
    public SearchParams explain() {
        return explain(true);
    }

    /**
     * Activates or deactivates the explanation of each result hit in the response, according to the parameter.
     *
     * @param explain should the response include an explanation of each hit (true) or not (false)?
     * @return this SearchParams for chaining.
     */
    public SearchParams explain(boolean explain) {
        this.explain = explain;
        return this;
    }

    /**
     * Configures the highlighting of matches in the response.
     *
     * This drives the inclusion of the {@link SearchQueryRow#fragments() fragments} in each {@link SearchQueryRow hit}.
     *
     * Note that to be highlighted, the fields must be stored in the FTS index.
     *
     * @param style the {@link HighlightStyle} to apply.
     * @param fields the optional fields on which to highlight. If none, all fields where there is a match are highlighted.
     * @return this SearchParams for chaining.
     */
    public SearchParams highlight(HighlightStyle style, String... fields) {
        this.highlightStyle = style;
        if (fields != null && fields.length > 0) {
            highlightFields = fields;
        }
        return this;
    }

    /**
     * Clears any previously configured highlighting.
     *
     * @return this SearchParams for chaining.
     * @see #highlight(HighlightStyle, String...)
     */
    public SearchParams clearHighlight() {
        this.highlightStyle = null;
        this.highlightFields = null;
        return this;
    }

    /**
     * Configures the list of fields for which the whole value should be included in the response. If empty, no field
     * values are included.
     *
     * This drives the inclusion of the {@link SearchQueryRow#fields() fields} in each {@link SearchQueryRow hit}.
     *
     * Note that to be highlighted, the fields must be stored in the FTS index.
     *
     * @param fields
     * @return this SearchParams for chaining.
     */
    public SearchParams fields(String... fields) {
        if (fields != null) {
            this.fields = fields;
        }
        return this;
    }

    /**
     * Adds one or more {@link SearchFacet} to the query.
     *
     * This is an additive operation (the given facets are added to any facet previously requested),
     * but if an existing facet has the same name it will be replaced.
     *
     * This drives the inclusion of the {@link SearchQueryResult#facets()} facets} in the {@link SearchQueryResult}.
     *
     * Note that to be faceted, a field's value must be stored in the FTS index.
     */
    public SearchParams addFacets(SearchFacet... facets) {
        if (facets != null) {
            for (SearchFacet facet : facets) {
                this.facets.put(facet.name(), facet);
            }
        }
        return this;
    }

    /**
     * Clears all previously added {@link SearchFacet}.
     *
     * @return this SearchParams for chaining.
     * @see #addFacets(SearchFacet...)
     */
    public SearchParams clearFacets() {
        this.facets.clear();
        return this;
    }

    /**
     * Sets the server side timeout. By default, the SDK will set this value to the configured
     * {@link CouchbaseEnvironment#searchTimeout() searchTimeout} from the environment.
     *
     * @param timeout the server side timeout to apply.
     * @param unit the unit for the timeout.
     * @return this SearchParams for chaining.
     */
    public SearchParams serverSideTimeout(long timeout, TimeUnit unit) {
        this.serverSideTimeout = unit.toMillis(timeout);
        return this;
    }

    /**
     * @return the value of the {@link #limit(int)} parameter, or null if it was not set.
     */
    public Integer getLimit() {
        return limit;
    }

    /**
     * @return the value of the {@link #skip(int)} parameter, or null if it was not set.
     */
    public Integer getSkip() {
        return skip;
    }

    /**
     * @return the value of the {@link #highlight(HighlightStyle, String...) highlight style} parameter,
     * or null if it was not set.
     */
    public HighlightStyle getHighlightStyle() {
        return highlightStyle;
    }

    /**
     * @return the value of the {@link #highlight(HighlightStyle, String...) highlight fields} parameter,
     * or an empty array if it was not set.
     */
    public String[] getHighlightFields() {
        return highlightFields;
    }

    /**
     * @return the value of the {@link #fields(String...)} parameter, or an empty array if it was not set.
     */
    public String[] getFields() {
        return fields;
    }

    /**
     * @return the Map of {@link #addFacets(SearchFacet...) facets}, or an empty Map if it was not set.
     */
    public Map<String, SearchFacet> getFacets() {
        return facets;
    }

    /**
     * @return the value of the {@link #serverSideTimeout(long, TimeUnit)} parameter, or null if it was not set.
     */
    public Long getServerSideTimeout() {
        return serverSideTimeout;
    }

    public void injectParams(JsonObject queryJson) {
        if (limit != null && limit >= 0) {
            queryJson.put("size", limit);
        }
        if (skip != null && skip >= 0) {
            queryJson.put("from", skip);
        }
        if (explain != null) {
            queryJson.put("explain", explain);
        }
        if (highlightStyle != null) {
            JsonObject highlight = JsonObject.create();
            highlight.put("style", highlightStyle.name().toLowerCase());
            if (highlightFields != null && highlightFields.length > 0) {
                highlight.put("fields", JsonArray.from(highlightFields));
            }
            queryJson.put("highlight", highlight);
        }
        if (fields != null && fields.length > 0) {
            queryJson.put("fields", JsonArray.from(fields));
        }
        if (!this.facets.isEmpty()) {
            JsonObject facets = JsonObject.create();
            for (SearchFacet f : this.facets.values()) {
                JsonObject facet = JsonObject.create();
                f.injectParams(facet);
                facets.put(f.name(), facet);
            }
            queryJson.put("facets", facets);
        }
        if(serverSideTimeout != null) {
            JsonObject control = JsonObject.empty();
            control.put("timeout", serverSideTimeout);
            queryJson.put("ctl", control);
        }
    }

}
