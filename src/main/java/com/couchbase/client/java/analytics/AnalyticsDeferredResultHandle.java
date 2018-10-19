/*
 * Copyright (c) 2018 Couchbase, Inc.
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

package com.couchbase.client.java.analytics;
import java.util.Iterator;
import java.util.List;

import com.couchbase.client.core.BackpressureException;
import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.RequestCancelledException;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.error.CouchbaseOutOfMemoryException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.error.QueryExecutionException;
import com.couchbase.client.java.error.TemporaryFailureException;

/**
 * An async handle to fetch the status and results of a deferred
 * Analytics Query
 *
 * @author Subhashni Balakrishnan
 * @since 2.7.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public interface AnalyticsDeferredResultHandle {

    /**
     * Get the status uri
     *
     * @return uri
     */
    @InterfaceAudience.Private
    String getStatusHandleUri();

    /**
     * Get the result uri if available
     * Throws {@link IllegalStateException} if there is no result handle available
     * @return uri
     */
    @InterfaceAudience.Private
    String getResultHandleUri();

    /**
     * @return the list of all {@link AnalyticsQueryRow}, the results of the query, if successful.
     *
     * Throws in the following circumstances
     *
     * - {@link QueryExecutionException} if there is no result URI available. A result URI is
     *  available only through the successful response of the status call. If the query status was still running,
     *  retrying the status call until success, would fetch the result URI.
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     */
    List<AnalyticsQueryRow> allRows();

    /**
     * @return an iterator over the list of all {@link AnalyticsQueryRow}, the results of the query, if successful.
     *
     * Throws in the following circumstances
     *
     * - {@link QueryExecutionException} if there is no result URI available. A result URI is
     *  available only through the successful response of the status call. If the query status was still running,
     *  retrying the status call until success, would fetch the result URI.
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     */
    Iterator<AnalyticsQueryRow> rows();

    /**
     * Returns the final status of the query. For example, a successful query will return "<code>success</code>"
     * Other statuses include (but are not limited to) "<code>running</code>" when the query is still in execution,
     * "<code>fatal</code>" when fatal errors occurred and "<code>timeout</code>" when the query timed out on the server
     * side but not yet on the client side. This method blocks until the query is over and the status can be established.
     *
     * Throws in the following circumstances
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     */
    String status();
}