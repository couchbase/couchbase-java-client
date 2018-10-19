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
import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import rx.Observable;

/**
 * An async handle to fetch the status and results of a deferred
 * Analytics Query
 *
 * @author Subhashni Balakrishnan
 * @since 2.7.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public interface AsyncAnalyticsDeferredResultHandle {

    @InterfaceAudience.Private
    String getStatusHandleUri();


    @InterfaceAudience.Private
    String getResultHandleUri();

    /**
     * @return an async stream of each row resulting from the query (empty if fatal errors occurred)
     * with custom timeout for deferred queries since it does a later fetch. If no timeout is specified,
     * it uses the default query timeout.
     * If the results are not yet available, throws
     */
    Observable<AsyncAnalyticsQueryRow> rows();

    /**
     * @return an async stream of each row resulting from the deferrred query (empty if fatal errors occurred)
     * with custom timeout for deferred queries since it does a later fetch. If no timeout is specified,
     * it uses the default query timeout.
     */
    Observable<AsyncAnalyticsQueryRow> rows(long timeout, TimeUnit timeunit);

    /**
     * @return the current status of the query execution retrieved from the server with default timeout
     */
    Observable<String> status();

    /**
     * @return the current status of the query execution retrieved from the server with custom timeout
     */
    Observable<String> status(long timeout, TimeUnit timeunit);
}
