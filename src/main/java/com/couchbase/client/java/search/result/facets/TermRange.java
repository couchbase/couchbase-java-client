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
package com.couchbase.client.java.search.result.facets;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;

/**
 * A range (or bucket) for a {@link TermFacetResult}.
 * Counts the number of occurrences of a given term.
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class TermRange {

    private final String name;
    private final long count;

    public TermRange(String name, long count) {
        this.name = name;
        this.count = count;
    }

    public String name() {
        return name;
    }

    public long count() {
        return count;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("name='").append(name).append('\'');
        sb.append(", count=").append(count);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TermRange termRange = (TermRange) o;

        if (count != termRange.count) {
            return false;
        }
        return name.equals(termRange.name);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (int) (count ^ (count >>> 32));
        return result;
    }
}
