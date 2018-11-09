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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;

/**
 * Represents the status of a FTS query.
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface SearchStatus {

    /**
     * the total number of FTS pindexes that were queried.
     */
    long totalCount();

    /**
     * the number of FTS pindexes queried that successfully answered.
     */
    long successCount();

    /**
     * the number of FTS pindexes queried that gave an error. If &gt; 0,
     * the {@link SearchQueryResult}'s {@link SearchQueryResult#errors()} method will contain errors.
     */
    long errorCount();

    /**
     * @return true if all FTS indexes answered successfully.
     */
    boolean isSuccess();
}
