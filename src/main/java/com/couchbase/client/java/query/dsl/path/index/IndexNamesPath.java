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
package com.couchbase.client.java.query.dsl.path.index;

import java.util.List;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.query.Index;
import com.couchbase.client.java.query.dsl.path.Path;

/**
 * Path of the Index building DSL to specify which index(es) to build.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public interface IndexNamesPath extends Path {

    /**
     * Specify the index or indexes in a pending state that needs building.
     *
     * @param indexName minimum index to build (name will be escaped).
     * @param indexNames 0-n additional indexes to also build (names will be escaped).
     */
    UsingPath indexes(String indexName, String... indexNames);

    /**
     * Specify the indexes in a pending state that needs building, as a non-empty list.
     *
     * @param indexNames the {@link List} of indexes to build (names will be escaped).
     */
    UsingPath indexes(List<String> indexNames);

    /**
     * Build the primary index (using a name of {@link Index#PRIMARY_NAME}, must be in a pending state).
     */
    UsingPath primary();
}
