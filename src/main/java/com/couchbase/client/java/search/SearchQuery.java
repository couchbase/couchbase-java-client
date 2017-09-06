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
import com.couchbase.client.java.MutationState;
import com.couchbase.client.java.document.Document;
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
import com.couchbase.client.java.search.queries.GeoBoundingBoxQuery;
import com.couchbase.client.java.search.queries.GeoDistanceQuery;
import com.couchbase.client.java.search.queries.MatchAllQuery;
import com.couchbase.client.java.search.queries.MatchNoneQuery;
import com.couchbase.client.java.search.queries.MatchPhraseQuery;
import com.couchbase.client.java.search.queries.MatchQuery;
import com.couchbase.client.java.search.queries.NumericRangeQuery;
import com.couchbase.client.java.search.queries.PhraseQuery;
import com.couchbase.client.java.search.queries.PrefixQuery;
import com.couchbase.client.java.search.queries.RegexpQuery;
import com.couchbase.client.java.search.queries.QueryStringQuery;
import com.couchbase.client.java.search.queries.TermQuery;
import com.couchbase.client.java.search.queries.TermRangeQuery;
import com.couchbase.client.java.search.queries.WildcardQuery;
import com.couchbase.client.java.search.result.SearchQueryResult;
import com.couchbase.client.java.search.result.SearchQueryRow;
import com.couchbase.client.java.search.sort.SearchSort;
import com.couchbase.client.java.subdoc.DocumentFragment;
import rx.Observable;

/**
 * The FTS API entry point. Describes an FTS query entirely (index, query body and parameters) and can
 * be used at the {@link Bucket} level to perform said query. Also has factory methods for all types of
 * fts queries (as in the various types a query body can have: term, match, conjunction, ...).
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Committed
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
    private JsonArray sort;
    private Map<String, SearchFacet> facets;
    private Long serverSideTimeout;
    private SearchConsistency consistency;
    private MutationState mutationState;

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
        this.sort = JsonArray.empty();
        this.facets = new HashMap<String, SearchFacet>();

        this.consistency = null;
        this.mutationState = null;
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
            if (highlightStyle != HighlightStyle.SERVER_DEFAULT) {
                highlight.put("style", highlightStyle.name().toLowerCase());
            }
            if (highlightFields != null && highlightFields.length > 0) {
                highlight.put("fields", JsonArray.from(highlightFields));
            }
            queryJson.put("highlight", highlight);
        }
        if (fields != null && fields.length > 0) {
            queryJson.put("fields", JsonArray.from(fields));
        }
        if (!sort.isEmpty()) {
            queryJson.put("sort", sort);
        }
        if (!this.facets.isEmpty()) {
            JsonObject facets = JsonObject.create();
            for (Map.Entry<String, SearchFacet> entry : this.facets.entrySet()) {
                JsonObject facetJson = JsonObject.create();
                entry.getValue().injectParams(facetJson);
                facets.put(entry.getKey(), facetJson);
            }
            queryJson.put("facets", facets);
        }

        JsonObject control = JsonObject.empty();
        //check need for timeout
        if(serverSideTimeout != null) {
            control.put("timeout", serverSideTimeout);
        }
        //check need for consistency
        if (consistency != null || mutationState != null) {
            JsonObject consistencyJson = JsonObject.create();

            if (consistency == SearchConsistency.NOT_BOUNDED) {
                consistencyJson.put("level", "");
            } else if (mutationState != null) {
                consistencyJson.put("level", "at_plus");
                consistencyJson.put("vectors", JsonObject.create().put(this.indexName, mutationState.exportForFts()));
            }
            control.put("consistency", consistencyJson);
        }
        //if any control was set, inject it
        if (!control.isEmpty()) {
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
     * Configures the highlighting of matches in the response, for the specified fields and using the server's default
     * highlighting style.
     *
     * This drives the inclusion of the {@link SearchQueryRow#fragments() fragments} in each {@link SearchQueryRow hit}.
     *
     * Note that to be highlighted, the fields must be stored in the FTS index.
     *
     * @param fields the optional fields on which to highlight. If none, all fields where there is a match are highlighted.
     * @return this SearchQuery for chaining.
     */
    public SearchQuery highlight(String... fields) {
        return highlight(HighlightStyle.SERVER_DEFAULT, fields);
    }

    /**
     * Configures the highlighting of matches in the response for all fields, using the server's default highlighting
     * style.
     *
     * This drives the inclusion of the {@link SearchQueryRow#fragments() fragments} in each {@link SearchQueryRow hit}.
     *
     * Note that to be highlighted, the fields must be stored in the FTS index.
     *
     * @return this SearchQuery for chaining.
     */
    public SearchQuery highlight() {
        return highlight(HighlightStyle.SERVER_DEFAULT);
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
     * Configures the list of fields (including special fields) which are used for sorting purposes. If empty, the
     * default sorting (descending by score) is used by the server.
     *
     * The list of sort fields can include actual fields (like "firstname" but then they must be stored in the index,
     * configured in the server side mapping). Fields provided first are considered first and in a "tie" case the
     * next sort field is considered. So sorting by "firstname" and then "lastname" will first sort ascending by
     * the firstname and if the names are equal then sort ascending by lastname. Special fields like "_id" and "_score"
     * can also be used. If prefixed with "-" the sort order is set to descending.
     *
     * If no sort is provided, it is equal to sort("-_score"), since the server will sort it by score in descending
     * order.
     *
     * @param sort the fields that should take part in the sorting.
     * @return this SearchQuery for chaining.
     */
    public SearchQuery sort(Object... sort) {
        if (sort != null) {
            for (Object o : sort) {
                if (o instanceof String) {
                    this.sort.add((String) o);
                } else if (o instanceof SearchSort) {
                    JsonObject params = JsonObject.create();
                    ((SearchSort) o).injectParams(params);
                    this.sort.add(params);
                } else if (o instanceof JsonObject) {
                    this.sort.add(o);
                } else {
                    throw new IllegalArgumentException("Only String ort SearchSort " +
                        "instances are allowed as sort arguments!");
                }
            }
        }
        return this;
    }

    /**
     * Adds one {@link SearchFacet} to the query.
     *
     * This is an additive operation (the given facets are added to any facet previously requested),
     * but if an existing facet has the same name it will be replaced.
     *
     * This drives the inclusion of the {@link SearchQueryResult#facets()} facets} in the {@link SearchQueryResult}.
     *
     * Note that to be faceted, a field's value must be stored in the FTS index.
     *
     * @param facetName the name of the facet to add (or replace if one already exists with same name).
     * @param facet the facet to add.
     */
    public SearchQuery addFacet(String facetName, SearchFacet facet) {
        if (facet == null || facetName == null) {
            throw new NullPointerException("Facet name and description must not be null");
        }
        this.facets.put(facetName,  facet);
        return this;
    }

    /**
     * Clears all previously added {@link SearchFacet}.
     *
     * @return this SearchQuery for chaining.
     * @see #addFacet(String, SearchFacet)
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
     * Sets the unparameterized consistency to consider for this FTS query. This replaces any
     * consistency tuning previously set.
     *
     * @param consistency the simple consistency to use.
     * @return this SearchQuery for chaining.
     */
    public SearchQuery searchConsistency(SearchConsistency consistency) {
        this.consistency = consistency;
        this.mutationState = null;
        return this;
    }

    /**
     * Sets the consistency to consider for this FTS query to AT_PLUS and
     * uses the mutation information from the given documents to parameterize
     * the consistency. This replaces any consistency tuning previously set.
     *
     * @param docs one or mode {@link Document} to get mutation state information from.
     * @return this SearchQuery for chaining.
     */
    public SearchQuery consistentWith(Document... docs) {
        this.consistency = null;
        this.mutationState = MutationState.from(docs);
        return this;
    }

    /**
     * Sets the consistency to consider for this FTS query to AT_PLUS and
     * uses the mutation information from the given document fragments to parameterize
     * the consistency. This replaces any consistency tuning previously set.
     *
     * @param fragments one or mode {@link DocumentFragment} to get mutation state information from.
     * @return this SearchQuery for chaining.
     */
    public SearchQuery consistentWith(DocumentFragment... fragments) {
        this.consistency = null;
        this.mutationState = MutationState.from(fragments);
        return this;
    }

    /**
     * Sets the consistency to consider for this FTS query to AT_PLUS and
     * uses the {@link MutationState} directly to parameterize the consistency.
     * This replaces any consistency tuning previously set.
     *
     * @param mutationState the {@link MutationState} information to work with.
     * @return this SearchQuery for chaining.
     */
    public SearchQuery consistentWith(MutationState mutationState) {
        this.consistency = null;
        this.mutationState = mutationState;
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
     * @return the Map of {@link #addFacet(String, SearchFacet) facets (by name)}, or an empty Map if it was not set.
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

    /** Prepare a {@link QueryStringQuery} body. */
    public static QueryStringQuery queryString(String query) {
        return new QueryStringQuery(query);
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

    /** Prepare a {@link TermRangeQuery} body. */
    public static TermRangeQuery termRange() {
        return new TermRangeQuery();
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

    /** Prepare a {@link GeoBoundingBoxQuery} body. */
    public static GeoBoundingBoxQuery geoBoundingBox(double topLeftLon, double topLeftLat,
        double bottomRightLon, double bottomRightLat) {
        return new GeoBoundingBoxQuery(topLeftLon, topLeftLat, bottomRightLon, bottomRightLat);
    }

    /** Prepare a {@link GeoDistanceQuery} body. */
    public static GeoDistanceQuery geoDistance(double locationLon, double locationLat, String distance) {
        return new GeoDistanceQuery(locationLon, locationLat, distance);
    }
}
