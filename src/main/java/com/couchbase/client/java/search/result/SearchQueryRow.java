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

import java.util.List;
import java.util.Map;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.search.HighlightStyle;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.search.result.hits.HitLocations;

/**
 * An FTS result row (or hit).
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface SearchQueryRow {

    /**
     * The name of the FTS pindex that gave this result.
     */
    String index();

    /**
     * The id of the matching document.
     */
    String id();

    /**
     * The score of this hit.
     */
    double score();

    /**
     * If {@link SearchQuery#explain() requested in the query}, an explanation of the match, in JSON form.
     */
    JsonObject explanation();

    /**
     * This hit's location, as an {@link HitLocations} map-like object.
     */
    HitLocations locations();

    /**
     * The fragments for each field that was requested as highlighted
     * (as defined in the {@link SearchQuery#highlight(HighlightStyle, String...) SearchParams}).
     *
     * A fragment is an extract of the field's value where the matching terms occur.
     * Matching terms are surrounded by a <code>&lt;match&gt;</code> tag.
     *
     * @return the fragments as a {@link Map}. Keys are the fields.
     */
    Map<String, List<String>> fragments();

    /**
     *The value of each requested field (as defined in the {@link SearchQuery}.
     * @return the fields values as a {@link Map}. Keys are the fields.
     */
    Map<String, String> fields();

}
