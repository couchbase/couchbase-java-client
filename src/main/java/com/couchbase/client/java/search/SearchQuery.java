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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.Bucket;
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
    private final SearchParams params;

    /**
     * Prepare an FTS {@link SearchQuery} on an index, without top-level query parameters.
     *
     * @param indexName the FTS index to search in.
     * @param queryPart the body of the FTS query (eg. a match phrase query).
     */
    public SearchQuery(String indexName, AbstractFtsQuery queryPart) {
        this(indexName, queryPart, null);
    }

    /**
     * Prepare an FTS {@link SearchQuery} on an index, with top-level query parameters.
     *
     * @param indexName the FTS index to search in.
     * @param queryPart the body of the FTS query (eg. a match phrase query).
     * @param params the top-level parameters for the query (eg. timeout, limit, facets...).
     */
    public SearchQuery(String indexName, AbstractFtsQuery queryPart, SearchParams params) {
        this.indexName = indexName;
        this.queryPart = queryPart;
        this.params = params;
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
     * @return the top-level {@link SearchParams search parameters} or null if not applicable.
     */
    public SearchParams params() {
        return params;
    }

    /* ===============================
     * Builder methods for FTS queries
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
