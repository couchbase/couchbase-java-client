/**
 * Copyright (C) 2015 Couchbase, Inc.
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
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
