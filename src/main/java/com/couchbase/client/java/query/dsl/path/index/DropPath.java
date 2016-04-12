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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.dsl.path.Path;

/**
 * Initial path of the Index dropping DSL.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public interface DropPath extends Path, Statement {

    /**
     * Drop one secondary indexes in the specified keyspace.
     *
     * @param keyspace the keyspace (bucket) in which we'll drop indexes (will be escaped).
     * @param indexName the name of the index to drop (will be escaped).
     */
    UsingPath drop(String keyspace, String indexName);

    /**
     * Drop one or more secondary indexes on the specified namespace:keyspace.
     *
     * @param namespace the namespace in which to work (will be escaped).
     * @param keyspace the keyspace (bucket) in which we'll drop indexes (will be escaped).
     * @param indexName the name of the index to drop (will be escaped).
     */
    UsingPath drop(String namespace, String keyspace, String indexName);

    /**
     * Drop the primary index in the specified keyspace.
     *
     * @param keyspace the keyspace (bucket) in which to drop primary index (will be escaped).
     */
    UsingPath dropPrimary(String keyspace);

    /**
     * Drop the primary index in the specified namespace:keyspace.
     *
     * @param namespace the namespace in which to work (will be escaped).
     * @param keyspace the keyspace (bucket) in which to drop primary index (will be escaped).
     */
    UsingPath dropPrimary(String namespace, String keyspace);
}
