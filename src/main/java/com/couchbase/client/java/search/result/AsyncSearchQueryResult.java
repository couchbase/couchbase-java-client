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

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.search.result.facets.FacetResult;
import rx.Observable;
import rx.Observer;
import rx.exceptions.CompositeException;

/**
 * The asynchronous interface for FTS query results.
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface AsyncSearchQueryResult {

    /**
     * The {@link SearchStatus} allows to check if the request was a full success ({@link SearchStatus#isSuccess()})
     * and gives more details about status for each queried index.
     */
    SearchStatus status();

    /**
     * An {@link Observable} of {@link SearchQueryRow rows} (or hits) describing each individual result. Note that
     * in case of a partial success, {@link Observer#onError(Throwable)} will be called <b>after</b> each partial
     * result has been emitted. In case of a full execution failure, no hit is emitted before the onError.
     *
     * The following execution-level exceptions can happen:
     *
     *  - if there is one or more execution-level errors, each of them is represented as a {@link RuntimeException},
     *    and all are aggregated into a single {@link CompositeException}.
     *  - if the request is malformed, the server side error message is returned as the message of a {@link CouchbaseException}.
     */
    Observable<SearchQueryRow> hits();

    /**
     * An {@link Observable} emitting a {@link FacetResult} for each requested facet in the original request.
     */
    Observable<FacetResult> facets();

    /**
     * An {@link Observable} asynchronously providing statistics about the request in the form of a single
     * {@link SearchMetrics}. Note that the metrics are emitted after all hits have been received.
     */
    Observable<SearchMetrics> metrics();
}
