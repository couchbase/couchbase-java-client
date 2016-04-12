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
import java.util.Iterator;
import java.util.List;

/**
 * @author Sergey Avseyev
 */
@InterfaceAudience.Public
@InterfaceStability.Experimental
public class SearchQueryResult implements Iterable<SearchQueryRow> {

    private final List<SearchQueryRow> hits;
    private final long took; // nanoseconds
    private final long totalHits;
    private final double maxScore;

    public SearchQueryResult(long took, long totalHits, double maxScore, List<SearchQueryRow> hits) {
        this.took = took;
        this.totalHits = totalHits;
        this.maxScore = maxScore;
        this.hits = hits;
    }

    public long took() {
        return took;
    }

    public long totalHits() {
        return totalHits;
    }

    public List<SearchQueryRow> hits() {
        return hits;
    }

    public double maxScore() {
        return maxScore;
    }

    @Override
    public Iterator<SearchQueryRow> iterator() {
        return hits.iterator();
    }
}
