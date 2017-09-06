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

import java.util.List;
import java.util.Map;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.search.result.SearchQueryRow;
import com.couchbase.client.java.search.result.hits.HitLocations;

/**
 * The default implementation for a {@link SearchQueryRow}
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public class DefaultSearchQueryRow implements SearchQueryRow {

    private final String index;
    private final String id;
    private final double score;
    private final JsonObject explanation;
    private final HitLocations locations;
    private final Map<String, List<String>> fragments;
    private final Map<String, String> fields;

    public DefaultSearchQueryRow(String index, String id, double score, JsonObject explanation, HitLocations locations,
            Map<String, List<String>> fragments, Map<String, String> fields) {
        this.index = index;
        this.id = id;
        this.score = score;
        this.explanation = explanation;
        this.locations = locations;
        this.fragments = fragments;
        this.fields = fields;
    }

    @Override
    public String index() {
        return this.index;
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public double score() {
        return this.score;
    }

    @Override
    public JsonObject explanation() {
        return this.explanation;
    }

    @Override
    public HitLocations locations() {
        return this.locations;
    }

    @Override
    public Map<String, List<String>> fragments() {
        return this.fragments;
    }

    @Override
    public Map<String, String> fields() {
        return this.fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultSearchQueryRow that = (DefaultSearchQueryRow) o;

        if (Double.compare(that.score, score) != 0) {
            return false;
        }
        if (!index.equals(that.index)) {
            return false;
        }
        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = index.hashCode();
        result = 31 * result + id.hashCode();
        temp = Double.doubleToLongBits(score);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "DefaultSearchQueryRow{" +
                "index='" + index + '\'' +
                ", id='" + id + '\'' +
                ", score=" + score +
                ", explanation=" + explanation +
                ", locations=" + locations +
                ", fragments=" + fragments +
                ", fields=" + fields +
                '}';
    }
}
