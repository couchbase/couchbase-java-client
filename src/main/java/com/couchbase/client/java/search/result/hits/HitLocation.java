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
package com.couchbase.client.java.search.result.hits;


import java.util.Arrays;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;

/**
 * A FTS result hit location indicates at which position a given term occurs inside a given field.
 * In case the field is an array, {@link #arrayPositions} will indicate which index/indices in the
 * array contain the term.
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Public
public class HitLocation {

    private final String field;
    private final String term;
    private final long pos;
    private final long start;
    private final long end;

    /**
     * can be null
     */
    private final long[] arrayPositions;

    public HitLocation(String field, String term, long pos, long start, long end, long[] arrayPositions) {
        this.field = field;
        this.term = term;
        this.pos = pos;
        this.start = start;
        this.end = end;
        this.arrayPositions = arrayPositions;
    }

    public HitLocation(String field, String term, long pos, long start, long end) {
        this(field, term, pos, start, end, null);
    }

    public String field() {
        return field;
    }

    public String term() {
        return term;
    }

    public long pos() {
        return pos;
    }

    public long start() {
        return start;
    }

    public long end() {
        return end;
    }

    /**
     * @return the array positions, or null if not applicable.
     */
    public long[] arrayPositions() {
        return arrayPositions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HitLocation that = (HitLocation) o;

        if (pos != that.pos) {
            return false;
        }
        if (start != that.start) {
            return false;
        }
        if (end != that.end) {
            return false;
        }
        if (!field.equals(that.field)) {
            return false;
        }
        if (!term.equals(that.term)) {
            return false;
        }
        return Arrays.equals(arrayPositions, that.arrayPositions);

    }

    @Override
    public int hashCode() {
        int result = field.hashCode();
        result = 31 * result + term.hashCode();
        result = 31 * result + (int) (pos ^ (pos >>> 32));
        result = 31 * result + (int) (start ^ (start >>> 32));
        result = 31 * result + (int) (end ^ (end >>> 32));
        result = 31 * result + Arrays.hashCode(arrayPositions);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder()
                .append("HitLocation{")
                .append("field='").append(field).append('\'')
                .append(", term='").append(term).append('\'')
                .append(", pos=").append(pos)
                .append(", start=").append(start)
                .append(", end=").append(end);

        if (arrayPositions != null) {
            sb.append(", arrayPositions=").append(Arrays.toString(arrayPositions));
        }

        sb.append('}');
        return sb.toString();
    }
}
