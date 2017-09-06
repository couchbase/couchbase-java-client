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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.search.result.SearchMetrics;

/**
 * The default implementation for a {@link SearchMetrics}
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public class DefaultSearchMetrics implements SearchMetrics {

    private final long took;
    private final long totalHits;
    private final double maxScore;

    public DefaultSearchMetrics(long took, long totalHits, double maxScore) {
        this.took = took;
        this.totalHits = totalHits;
        this.maxScore = maxScore;
    }

    @Override
    public long took() {
        return this.took;
    }

    @Override
    public long totalHits() {
        return this.totalHits;
    }

    @Override
    public double maxScore() {
        return this.maxScore;
    }

    @Override
    public String toString() {
        return "DefaultSearchMetrics{" +
                "took=" + took +
                ", totalHits=" + totalHits +
                ", maxScore=" + maxScore +
                '}';
    }
}
