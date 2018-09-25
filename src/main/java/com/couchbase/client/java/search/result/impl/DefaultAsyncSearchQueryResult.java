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
package com.couchbase.client.java.search.result.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.FtsConsistencyTimeoutException;
import com.couchbase.client.java.error.FtsMalformedRequestException;
import com.couchbase.client.java.error.FtsServerOverloadException;
import com.couchbase.client.java.error.IndexDoesNotExistException;
import com.couchbase.client.java.search.result.AsyncSearchQueryResult;
import com.couchbase.client.java.search.result.SearchMetrics;
import com.couchbase.client.java.search.result.SearchQueryRow;
import com.couchbase.client.java.search.result.SearchStatus;
import com.couchbase.client.java.search.result.facets.DateRange;
import com.couchbase.client.java.search.result.facets.DefaultDateRangeFacetResult;
import com.couchbase.client.java.search.result.facets.DefaultNumericRangeFacetResult;
import com.couchbase.client.java.search.result.facets.DefaultTermFacetResult;
import com.couchbase.client.java.search.result.facets.FacetResult;
import com.couchbase.client.java.search.result.facets.NumericRange;
import com.couchbase.client.java.search.result.facets.TermRange;
import com.couchbase.client.java.search.result.hits.DefaultHitLocations;
import com.couchbase.client.java.search.result.hits.HitLocations;
import rx.Observable;
import rx.exceptions.CompositeException;

/**
 * The default implementation for an {@link AsyncSearchQueryResult}
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public class DefaultAsyncSearchQueryResult implements AsyncSearchQueryResult {

    private final SearchStatus status;
    private final Observable<SearchQueryRow> hits;
    private final Observable<FacetResult> facets;
    private final Observable<SearchMetrics> metrics;

    public DefaultAsyncSearchQueryResult(SearchStatus status,
            Observable<SearchQueryRow> hits, Observable<FacetResult> facets,
            Observable<SearchMetrics> metrics) {
        this.status = status;
        this.hits = hits;
        this.facets = facets;
        this.metrics = metrics;
    }

    @Override
    public SearchStatus status() {
        return status;
    }

    @Override
    public Observable<SearchQueryRow> hits() {
        return hits;
    }

    @Override
    public Observable<FacetResult> facets() {
        return facets;
    }

    @Override
    public Observable<SearchMetrics> metrics() {
        return metrics;
    }

    /**
     * Utility method to extract an {@link AsyncSearchQueryResult} from a JSON representation of the
     * whole search service response.
     *
     * @param json the whole response, as returned by the search service.
     * @return the corresponding {@link AsyncSearchQueryResult}.
     * @deprecated FTS is still in BETA so the response format is likely to change in a future version
     */
    @Deprecated
    public static AsyncSearchQueryResult fromJson(JsonObject json) {
        JsonObject jsonStatus = json.getObject("status");
        SearchStatus status = new DefaultSearchStatus(
                jsonStatus.getLong("total"),
                jsonStatus.getLong("failed"),
                jsonStatus.getLong("successful"));

        long totalHits = json.getLong("total_hits");
        long took = json.getLong("took");
        double maxScore = json.getDouble("max_score");
        SearchMetrics metrics = new DefaultSearchMetrics(took, totalHits, maxScore);

        List<SearchQueryRow> hits = new ArrayList<SearchQueryRow>();

        JsonArray rawHits = json.getArray("hits");
        if (rawHits != null) {
            for (Object rawHit : rawHits) {
                JsonObject hit = (JsonObject) rawHit;
                String index = hit.getString("index");
                String id = hit.getString("id");
                double score = hit.getDouble("score");
                JsonObject explanationJson = hit.getObject("explanation");
                if (explanationJson == null) {
                    explanationJson = JsonObject.empty();
                }

                HitLocations locations = DefaultHitLocations.from(hit.getObject("locations"));

                JsonObject fragmentsJson = hit.getObject("fragments");
                Map<String, List<String>> fragments;
                if (fragmentsJson != null) {
                    fragments = new HashMap<String, List<String>>(fragmentsJson.size());
                    for (String field : fragmentsJson.getNames()) {
                        List<String> fragment;
                        JsonArray fragmentJson = fragmentsJson.getArray(field);
                        if (fragmentJson != null) {
                            fragment = new ArrayList<String>(fragmentJson.size());
                            for (int i = 0; i < fragmentJson.size(); i++) {
                                fragment.add(fragmentJson.getString(i));
                            }
                        } else {
                            fragment = Collections.emptyList();
                        }
                        fragments.put(field, fragment);
                    }
                } else {
                    fragments = Collections.emptyMap();
                }

                Map<String, String> fields;
                JsonObject fieldsJson = hit.getObject("fields");
                if (fieldsJson != null) {
                    fields = new HashMap<String, String>(fieldsJson.size());
                    for (String f : fieldsJson.getNames()) {
                        fields.put(f, String.valueOf(fieldsJson.get(f)));
                    }
                } else {
                    fields = Collections.emptyMap();
                }

                hits.add(new DefaultSearchQueryRow(index, id, score, explanationJson, locations, fragments, fields));
            }
        }

        List<FacetResult> facets;
        JsonObject facetsJson = json.getObject("facets");
        if (facetsJson != null) {
            facets = new ArrayList<FacetResult>(facetsJson.size());
            for (String facetName : facetsJson.getNames()) {
                JsonObject facetJson = facetsJson.getObject(facetName);
                String field = facetJson.getString("field");
                long total = facetJson.getLong("total");
                long missing = facetJson.getLong("missing");
                long other = facetJson.getLong("other");

                if (facetJson.containsKey("numeric_ranges")) {
                    JsonArray rangesJson = facetJson.getArray("numeric_ranges");
                    List<NumericRange> nr = new ArrayList<NumericRange>(rangesJson.size());
                    for (Object o : rangesJson) {
                        JsonObject r = (JsonObject) o;
                        nr.add(new NumericRange(r.getString("name"), r.getDouble("min"), r.getDouble("max"), r.getLong("count")));
                    }
                    facets.add(new DefaultNumericRangeFacetResult(facetName, field, total, missing, other, nr));
                } else if (facetJson.containsKey("date_ranges")) {
                    JsonArray rangesJson = facetJson.getArray("date_ranges");
                    List<DateRange> dr = new ArrayList<DateRange>(rangesJson.size());
                    for (Object o : rangesJson) {
                        JsonObject r = (JsonObject) o;
                        dr.add(new DateRange(r.getString("name"), r.getString("start"), r.getString("end"),
                                r.getLong("count")));
                    }
                    facets.add(new DefaultDateRangeFacetResult(facetName, field, total, missing, other, dr));
                } else {
                    List<TermRange> tr;
                    JsonArray rangesJson = facetJson.getArray("terms");
                    if (rangesJson == null) {
                        tr = Collections.emptyList();
                    } else {
                        tr = new ArrayList<TermRange>(rangesJson.size());
                        for (Object o : rangesJson) {
                            JsonObject r = (JsonObject) o;
                            tr.add(new TermRange(r.getString("term"), r.getLong("count")));
                        }
                    }
                    facets.add(new DefaultTermFacetResult(facetName, field, total, missing, other, tr));
                }
            }
        } else {
            facets = Collections.emptyList();
        }

        Observable<SearchQueryRow> errors;
        Object errorsRaw = jsonStatus.get("errors");
        if (errorsRaw instanceof JsonArray) {
            JsonArray errorsJson = (JsonArray) errorsRaw;
            List<Exception> exceptions = new ArrayList<Exception>(errorsJson.size());
            for (Object o : errorsJson) {
                exceptions.add(new RuntimeException(String.valueOf(o)));
            }
            errors = Observable.error(new CompositeException(exceptions));
        } else if (errorsRaw instanceof JsonObject) {
            JsonObject errorsJson = (JsonObject) errorsRaw;
            List<Exception> exceptions = new ArrayList<Exception>(errorsJson.size());
            for (String key : errorsJson.getNames()) {
                exceptions.add(new RuntimeException(key + ": " + errorsJson.get(key)));
            }
            errors = Observable.error(new CompositeException(exceptions));
        } else {
            errors = Observable.empty();
        }

        return new DefaultAsyncSearchQueryResult(status,
                Observable.from(hits).concatWith(errors),
                Observable.from(facets),
                Observable.just(metrics));

    }

    /**
     * A utility method to convert an HTTP 400 response from the search service into a proper
     * {@link AsyncSearchQueryResult}. HTTP 400 indicates the request was malformed and couldn't
     * be parsed on the server. As of Couchbase Server 4.5 such a response is a text/plain
     * body that describes the parsing error. The whole body is emitted/thrown, wrapped in a
     * {@link FtsMalformedRequestException}.
     *
     * @param payload the HTTP 400 response body describing the parsing failure.
     * @return an {@link AsyncSearchQueryResult} that will emit a {@link FtsMalformedRequestException} when calling its
     * {@link AsyncSearchQueryResult#hits() hits()} method.
     * @deprecated FTS is still in BETA so the response format is likely to change in a future version, and be
     * unified with the HTTP 200 response format.
     */
    @Deprecated
    public static AsyncSearchQueryResult fromHttp400(String payload) {
        //dummy default values
        SearchStatus status = new DefaultSearchStatus(1L, 1L, 0L);
        SearchMetrics metrics = new DefaultSearchMetrics(0L, 0L, 0d);


        return new DefaultAsyncSearchQueryResult(
                status,
                Observable.<SearchQueryRow>error(new FtsMalformedRequestException(payload)),
                Observable.<FacetResult>empty(),
                Observable.just(metrics)
        );
    }

    /**
     * A utility method to convert an HTTP 412 response from the search service into a proper
     * {@link AsyncSearchQueryResult}. HTTP 412 indicates the request couldn't be satisfied with given
     * consistency before the timeout expired. This is translated to a {@link FtsConsistencyTimeoutException}.
     *
     * @return an {@link AsyncSearchQueryResult} that will emit a {@link FtsConsistencyTimeoutException} when calling
     * its {@link AsyncSearchQueryResult#hits() hits()} method.
     * @deprecated FTS is still in BETA so the response format is likely to change in a future version, and be
     * unified with the HTTP 200 response format.
     */
    @Deprecated
    public static AsyncSearchQueryResult fromHttp412() {
        //dummy default values
        SearchStatus status = new DefaultSearchStatus(1L, 1L, 0L);
        SearchMetrics metrics = new DefaultSearchMetrics(0L, 0L, 0d);


        return new DefaultAsyncSearchQueryResult(
                status,
                Observable.<SearchQueryRow>error(new FtsConsistencyTimeoutException()),
                Observable.<FacetResult>empty(),
                Observable.just(metrics)
        );
    }

    /**
     * Creates a result out of the http 429 response code if retry didn't work.
     */
    public static AsyncSearchQueryResult fromHttp429(String payload) {
        SearchStatus status = new DefaultSearchStatus(1L, 1L, 0L);
        SearchMetrics metrics = new DefaultSearchMetrics(0L, 0L, 0d);

        return new DefaultAsyncSearchQueryResult(
                status,
                Observable.<SearchQueryRow>error(new FtsServerOverloadException(payload)),
                Observable.<FacetResult>empty(),
                Observable.just(metrics)
        );
    }

    /**
     * A utility method to return a result when the index is not found.
     *
     * @return an {@link AsyncSearchQueryResult} that will emit a {@link IndexDoesNotExistException} when calling
     * its {@link AsyncSearchQueryResult#hits() hits()} method.
     * @deprecated FTS is still in BETA so the response format is likely to change in a future version, and be
     * unified with the HTTP 200 response format.
     */
    @Deprecated
    public static AsyncSearchQueryResult fromIndexNotFound(final String indexName) {
        //dummy default values
        SearchStatus status = new DefaultSearchStatus(1L, 1L, 0L);
        SearchMetrics metrics = new DefaultSearchMetrics(0L, 0L, 0d);


        return new DefaultAsyncSearchQueryResult(
            status,
            Observable.<SearchQueryRow>error(new IndexDoesNotExistException("Search Index \"" + indexName
                + "\" Not Found")),
            Observable.<FacetResult>empty(),
            Observable.just(metrics)
        );
    }
}
