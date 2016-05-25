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
package com.couchbase.client.java.search;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.search.facet.SearchFacet;
import com.couchbase.client.java.search.queries.AbstractFtsQuery;
import com.couchbase.client.java.search.queries.BooleanFieldQuery;
import com.couchbase.client.java.search.queries.BooleanQuery;
import com.couchbase.client.java.search.queries.ConjunctionQuery;
import com.couchbase.client.java.search.queries.DateRangeQuery;
import com.couchbase.client.java.search.queries.DisjunctionQuery;
import com.couchbase.client.java.search.queries.DocIdQuery;
import com.couchbase.client.java.search.queries.MatchAllQuery;
import com.couchbase.client.java.search.queries.MatchNoneQuery;
import com.couchbase.client.java.search.queries.MatchPhraseQuery;
import com.couchbase.client.java.search.queries.MatchQuery;
import com.couchbase.client.java.search.queries.NumericRangeQuery;
import com.couchbase.client.java.search.queries.PhraseQuery;
import com.couchbase.client.java.search.queries.PrefixQuery;
import com.couchbase.client.java.search.queries.RegexpQuery;
import com.couchbase.client.java.search.queries.StringQuery;
import com.couchbase.client.java.search.queries.TermQuery;
import com.couchbase.client.java.search.queries.WildcardQuery;
import com.couchbase.client.java.search.result.SearchQueryResult;
import com.couchbase.client.java.search.result.SearchQueryRow;

/**
 * The FTS API entry point. Describes an FTS query entirely (index, query body and parameters) and can
 * be used at the {@link Bucket} level to perform said query. Also has factory methods for all types of
 * fts queries (as in the various types a query body can have: term, match, conjunction, ...).
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class SearchQuery {

    private final String indexName;
    private final AbstractFtsQuery queryPart;

    //top level search parameters
    private Integer limit;
    private Integer skip;
    private Boolean explain;
    private HighlightStyle highlightStyle;
    private String[] highlightFields;
    private String[] fields;
    private Map<String, SearchFacet> facets;
    private Long serverSideTimeout;

    /**
     * Prepare an FTS {@link SearchQuery} on an index. Top level query parameters can be set after that
     * by using the fluent API.
     *
     * @param indexName the FTS index to search in.
     * @param queryPart the body of the FTS query (eg. a match phrase query).
     */
    public SearchQuery(String indexName, AbstractFtsQuery queryPart) {
        this.indexName = indexName;
        this.queryPart = queryPart;

        this.highlightFields = new String[0];
        this.fields = new String[0];
        this.facets = new HashMap<String, SearchFacet>();
    }

    /**
     * @return the name of the index targeted by this query.
     */
    public String indexName() {
        return indexName;
    }

    /**
     * @return the actual query body.
     */
    public AbstractFtsQuery query() {
        return queryPart;
    }

    /**
     * Exports the whole query as a {@link JsonObject}.
     *
     * @see #injectParams(JsonObject) for the part that deals with global parameters
     * @see AbstractFtsQuery#injectParamsAndBoost(JsonObject) for the part that deals with the "query" entry
     */
    public JsonObject export() {
        JsonObject result = JsonObject.create();
        injectParams(result);

        JsonObject queryJson = JsonObject.create();
        queryPart.injectParamsAndBoost(queryJson);
        return result.put("query", queryJson);
    }

    /**
     * Inject the top level parameters of a query into a prepared {@link JsonObject}
     * that represents the root of the query.
     *
     * @param queryJson the prepared {@link JsonObject} for the whole query.
     */
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


    /* =====================================
     * Search parameter builders and getters
     * ===================================== */

    /**
     * Add a limit to the query on the number of hits it can return.
     *
     * @param limit the maximum number of hits to return.
     * @return this SearchQuery for chaining.
     */
    public SearchQuery limit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Set the number of hits to skip (eg. for pagination).
     *
     * @param skip the number of results to skip.
     * @return this SearchQuery for chaining.
     */
    public SearchQuery skip(int skip) {
        this.skip = skip;
        return this;
    }

    /**
     * Activates the explanation of each result hit in the response.
     *
     * @return this SearchQuery for chaining.
     */
    public SearchQuery explain() {
        return explain(true);
    }

    /**
     * Activates or deactivates the explanation of each result hit in the response, according to the parameter.
     *
     * @param explain should the response include an explanation of each hit (true) or not (false)?
     * @return this SearchQuery for chaining.
     */
    public SearchQuery explain(boolean explain) {
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
     * @return this SearchQuery for chaining.
     */
    public SearchQuery highlight(HighlightStyle style, String... fields) {
        this.highlightStyle = style;
        if (fields != null && fields.length > 0) {
            highlightFields = fields;
        }
        return this;
    }

    /**
     * Clears any previously configured highlighting.
     *
     * @return this SearchQuery for chaining.
     * @see #highlight(HighlightStyle, String...)
     */
    public SearchQuery clearHighlight() {
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
     * @return this SearchQuery for chaining.
     */
    public SearchQuery fields(String... fields) {
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
    public SearchQuery addFacets(SearchFacet... facets) {
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
     * @return this SearchQuery for chaining.
     * @see #addFacets(SearchFacet...)
     */
    public SearchQuery clearFacets() {
        this.facets.clear();
        return this;
    }

    /**
     * Sets the server side timeout. By default, the SDK will set this value to the configured
     * {@link CouchbaseEnvironment#searchTimeout() searchTimeout} from the environment.
     *
     * @param timeout the server side timeout to apply.
     * @param unit the unit for the timeout.
     * @return this SearchQuery for chaining.
     */
    public SearchQuery serverSideTimeout(long timeout, TimeUnit unit) {
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

    /* ===============================
     * Factory methods for FTS queries
     * =============================== */

    /** Prepare a {@link StringQuery} body. */
    public static StringQuery string(String query) {
        return new StringQuery(query);
    }

    /** Prepare a {@link MatchQuery} body. */
    public static MatchQuery match(String match) {
        return new MatchQuery(match);
    }

    /** Prepare a {@link MatchPhraseQuery} body. */
    public static MatchPhraseQuery matchPhrase(String matchPhrase) {
        return new MatchPhraseQuery(matchPhrase);
    }

    /** Prepare a {@link PrefixQuery} body. */
    public static PrefixQuery prefix(String prefix) {
        return new PrefixQuery(prefix);
    }

    /** Prepare a {@link RegexpQuery} body. */
    public static RegexpQuery regexp(String regexp) {
        return new RegexpQuery(regexp);
    }

    /** Prepare a {@link NumericRangeQuery} body. */
    public static NumericRangeQuery numericRange() {
        return new NumericRangeQuery();
    }

    /** Prepare a {@link DateRangeQuery} body. */
    public static DateRangeQuery dateRange() {
        return new DateRangeQuery();
    }

    /** Prepare a {@link DisjunctionQuery} body. */
    public static DisjunctionQuery disjuncts(AbstractFtsQuery... queries) {
        return new DisjunctionQuery(queries);
    }

    /** Prepare a {@link ConjunctionQuery} body. */
    public static ConjunctionQuery conjuncts(AbstractFtsQuery... queries) {
        return new ConjunctionQuery(queries);
    }

    /** Prepare a {@link BooleanQuery} body. */
    public static BooleanQuery booleans() {
        return new BooleanQuery();
    }

    /** Prepare a {@link WildcardQuery} body. */
    public static WildcardQuery wildcard(String wildcard) {
        return new WildcardQuery(wildcard);
    }

    /** Prepare a {@link DocIdQuery} body. */
    public static DocIdQuery docId(String... docIds) {
        return new DocIdQuery(docIds);
    }

    /** Prepare a {@link BooleanFieldQuery} body. */
    public static BooleanFieldQuery booleanField(boolean value) {
        return new BooleanFieldQuery(value);
    }

    /** Prepare a {@link TermQuery} body. */
    public static TermQuery term(String term) {
        return new TermQuery(term);
    }

    /** Prepare a {@link PhraseQuery} body. */
    public static PhraseQuery phrase(String... terms) {
        return new PhraseQuery(terms);
    }

    /** Prepare a {@link MatchAllQuery} body. */
    public static MatchAllQuery matchAll() {
        return new MatchAllQuery();
    }

    /** Prepare a {@link MatchNoneQuery} body. */
    public static MatchNoneQuery matchNone() {
        return new MatchNoneQuery();
    }
}
