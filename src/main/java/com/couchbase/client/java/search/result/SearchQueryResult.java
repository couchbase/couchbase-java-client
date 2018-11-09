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
package com.couchbase.client.java.search.result;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.search.facet.SearchFacet;
import com.couchbase.client.java.search.result.facets.FacetResult;
import rx.exceptions.CompositeException;

/**
 * The main interface for FTS query results. It is also an {@link Iterable Iterable&lt;SearchQueryRow&gt;},
 * where iteration is similar to iterating over {@link #hitsOrFail()}.
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface SearchQueryResult extends Iterable<SearchQueryRow> {

    /**
     * The {@link SearchStatus} allows to check if the request was a full success ({@link SearchStatus#isSuccess()})
     * and gives more details about status for each queried index.
     */
    SearchStatus status();

    /**
     * The list of FTS result rows, or "hits", for the FTS query. This method always returns
     * a list, including when an execution error (eg. partial results) occurred.
     *
     * @see #hitsOrFail() for a variant that throws an exception whenever execution errors have occurred.
     * @see #errors() to get a list of execution errors in JSON form.
     */
    List<SearchQueryRow> hits();

    /**
     * The list of FTS result rows, or "hits", for the FTS query. In case of an execution error
     * (eg. partial results), a {@link CompositeException} is thrown instead.
     *
     * @see #hits() for a variant that lists partial results instead of throwing the exception.
     * @see #errors() to get a list of execution errors in JSON form.
     */
    List<SearchQueryRow> hitsOrFail();

    /**
     * When an execution error happens (including partial results), this method returns a {@link List} of
     * the error(s) in {@link JsonObject JSON format}.
     *
     * @see #hits() to get results, including partial results (rather than throwing an exception).
     * @see #hitsOrFail() to get only full results, but throwing an exception whenever execution errors have occurred.
     */
    List<String> errors();

    /**
     * If one or more facet were requested via the {@link SearchQuery#addFacet(String, SearchFacet)} method
     * when querying, contains the result of each facet.
     *
     * <p>The map keys are the names given to each requested facet.</p>
     */
    Map<String, FacetResult> facets();

    /**
     * Provides a {@link SearchMetrics} object giving statistics on the request like number of hits, total time taken...
     */
    SearchMetrics metrics();

    /**
     * Returns an iterator over the hits ({@link SearchQueryRow}).
     * If an execution error happened, the corresponding exception is
     * thrown instead (same as attempting to iterate over {@link #hitsOrFail()}).
     *
     * @return an Iterator of rows/hits.
     */
    @Override
    Iterator<SearchQueryRow> iterator();
}
