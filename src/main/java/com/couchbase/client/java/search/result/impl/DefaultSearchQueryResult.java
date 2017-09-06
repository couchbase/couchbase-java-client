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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.search.result.AsyncSearchQueryResult;
import com.couchbase.client.java.search.result.SearchMetrics;
import com.couchbase.client.java.search.result.SearchQueryResult;
import com.couchbase.client.java.search.result.SearchQueryRow;
import com.couchbase.client.java.search.result.SearchStatus;
import com.couchbase.client.java.search.result.facets.FacetResult;
import rx.Observable;
import rx.exceptions.CompositeException;
import rx.functions.Func1;
import rx.functions.Func5;

/**
 * The default implementation for a {@link SearchQueryResult}
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public class DefaultSearchQueryResult implements SearchQueryResult {

    private final RuntimeException error;
    private final SearchStatus status;
    private final List<SearchQueryRow> hits;
    private final List<String> errors;
    private final Map<String, FacetResult> facets;
    private final SearchMetrics metrics;

    public DefaultSearchQueryResult(SearchStatus status,
            List<SearchQueryRow> hits, Throwable error,
            Map<String, FacetResult> facets, SearchMetrics metrics) {
        this.status = status;
        this.hits = hits;
        this.facets = facets;
        this.metrics = metrics;

        if (error == null) {
            this.error = null;
        } else if (error instanceof RuntimeException) {
            this.error = (RuntimeException) error;
        } else {
            this.error = new RuntimeException(error);
        }

        if (error instanceof CompositeException) {
            CompositeException composite = (CompositeException) error;
            this.errors = new ArrayList<String>(composite.getExceptions().size());
            for (Throwable e : composite.getExceptions()) {
                this.errors.add(e.getMessage());
            }
        } else if (error != null) {
            this.errors = Collections.singletonList(error.getMessage());
        } else {
            this.errors = Collections.emptyList();
        }
    }

    @Override
    public SearchStatus status() {
        return this.status;
    }

    @Override
    public List<SearchQueryRow> hits() {
        return this.hits;
    }

    @Override
    public List<SearchQueryRow> hitsOrFail() {
        if (error != null) {
            throw error;
        }
        return this.hits;
    }

    @Override
    public Iterator<SearchQueryRow> iterator() {
        return hitsOrFail().iterator();
    }

    @Override
    public List<String> errors() {
        return this.errors;
    }

    @Override
    public Map<String, FacetResult> facets() {
        return this.facets;
    }

    @Override
    public SearchMetrics metrics() {
        return this.metrics;
    }

    /**
     * Utility {@link Func1} to convert an {@link AsyncSearchQueryResult} into a synchronous {@link SearchQueryResult}.
     */
    public static final Func1<AsyncSearchQueryResult, Observable<SearchQueryResult>> FROM_ASYNC =
            new Func1<AsyncSearchQueryResult, Observable<SearchQueryResult>>() {
                @Override
                public Observable<SearchQueryResult> call(AsyncSearchQueryResult asqr) {
                    return Observable.zip(
                            Observable.just(asqr.status()),
                            asqr.hits()
                                    .onErrorResumeNext(Observable.<SearchQueryRow>empty())
                                    .toList(),
                            asqr.hits()
                                    .ignoreElements()
                                    .cast(Throwable.class)
                                    .onErrorResumeNext(new Func1<Throwable, Observable<Throwable>>() {
                                        @Override
                                        public Observable<Throwable> call(Throwable throwable) {
                                            return Observable.just(throwable);
                                        }
                                    })
                                    .singleOrDefault(null),
                            asqr.facets()
                                    .toMap(new Func1<FacetResult, String>() {
                                        @Override
                                        public String call(FacetResult facetResult) {
                                            return facetResult.name();
                                        }
                                    }),
                            asqr.metrics(),
                            new Func5<SearchStatus, List<SearchQueryRow>, Throwable, Map<String, FacetResult>, SearchMetrics, SearchQueryResult>() {
                                @Override
                                public SearchQueryResult call(SearchStatus searchStatus,
                                        List<SearchQueryRow> searchQueryRows, Throwable error,
                                        Map<String, FacetResult> facets, SearchMetrics searchMetrics) {
                                    return new DefaultSearchQueryResult(searchStatus, searchQueryRows, error,
                                            facets, searchMetrics);
                                }
                            });
                }
            };

    @Override
    public String toString() {
        return "DefaultSearchQueryResult{" +
                "error=" + error +
                ", status=" + status +
                ", hits=" + hits +
                ", errors=" + errors +
                ", facets=" + facets +
                ", metrics=" + metrics +
                '}';
    }
}
