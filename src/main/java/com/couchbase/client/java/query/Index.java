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
package com.couchbase.client.java.query;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.query.dsl.path.index.BuildIndexPath;
import com.couchbase.client.java.query.dsl.path.index.DefaultBuildIndexPath;
import com.couchbase.client.java.query.dsl.path.index.DefaultCreateIndexPath;
import com.couchbase.client.java.query.dsl.path.index.DefaultDropPath;
import com.couchbase.client.java.query.dsl.path.index.IndexType;
import com.couchbase.client.java.query.dsl.path.index.OnPath;
import com.couchbase.client.java.query.dsl.path.index.OnPrimaryPath;
import com.couchbase.client.java.query.dsl.path.index.UsingPath;

/**
 * DSL starting point for creating and managing N1QL indexes.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class Index {

    /**
     * The expected name given to the primary indexes by the server.
     */
    public static final String PRIMARY_NAME = "#primary";

    private Index() {}

    /**
     * Create a new secondary index.
     *
     * @param indexName the name of the new index (will be escaped).
     */
    public static OnPath createIndex(String indexName) {
        return new DefaultCreateIndexPath().create(indexName);
    }

    /**
     * Create a new primary index.
     */
    public static OnPrimaryPath createPrimaryIndex() {
        return new DefaultCreateIndexPath().createPrimary();
    }

    /**
     * Create a new primary index with a custom name.
     */
    public static OnPrimaryPath createNamedPrimaryIndex(String customPrimaryName) {
        return new DefaultCreateIndexPath().createPrimary(customPrimaryName);
    }

    /**
     * Triggers building of indexes that have been deferred. Note that this feature is currently
     * only supported for GSI indexes, so the final {@link UsingPath using clause} should be explicit and use
     * the {@link IndexType#GSI GSI index type}.
     */
    public static BuildIndexPath buildIndex() {
        return new DefaultBuildIndexPath();
    }

    /**
     * Drop a secondary index in the given namespace:keyspace.
     *
     * @param namespace the namespace prefix (will be escaped).
     * @param keyspace the keyspace (bucket, will be escaped).
     * @param indexName the name of the index to be dropped (will be escaped).
     */
    public static UsingPath dropIndex(String namespace, String keyspace, String indexName) {
        return new DefaultDropPath().drop(namespace, keyspace, indexName);
    }

    /**
     * Drop a secondary index in the given keyspace.
     *
     * @param keyspace the keyspace (bucket, will be escaped).
     * @param indexName the name of the index to be dropped (will be escaped).
     */
    public static UsingPath dropIndex(String keyspace, String indexName) {
        return new DefaultDropPath().drop(keyspace, indexName);
    }

    /**
     * Drop the primary index of the given namespace:keyspace.
     *
     * @param namespace the namespace prefix (will be escaped).
     * @param keyspace the keyspace (bucket, will be escaped).
     * @see #dropNamedPrimaryIndex(String, String, String) if the primary index name has been customized.
     */
    public static UsingPath dropPrimaryIndex(String namespace, String keyspace) {
        return new DefaultDropPath().dropPrimary(namespace, keyspace);
    }

    /**
     * Drop the primary index in the given keyspace.
     *
     * @param keyspace the keyspace (bucket, will be escaped).
     * @see #dropNamedPrimaryIndex(String, String) if the primary index name has been customized.
     */
    public static UsingPath dropPrimaryIndex(String keyspace) {
        return new DefaultDropPath().dropPrimary(keyspace);
    }


    /**
     * Drop the primary index of the given namespace:keyspace that has a custom name.
     *
     * @param namespace the namespace prefix (will be escaped).
     * @param keyspace the keyspace (bucket, will be escaped).
     * @param customPrimaryName the custom name for the primary index (will be escaped).
     */
    public static UsingPath dropNamedPrimaryIndex(String namespace, String keyspace, String customPrimaryName) {
        //N1QL syntax for dropping a primary with a name is actually similar to dropping secondary index
        return new DefaultDropPath().drop(namespace, keyspace, customPrimaryName);
    }

    /**
     * Drop the primary index in the given keyspace that has a custom name.
     *
     * @param keyspace the keyspace (bucket, will be escaped).
     * @param customPrimaryName the custom name for the primary index (will be escaped).
     */
    public static UsingPath dropNamedPrimaryIndex(String keyspace, String customPrimaryName) {
        //N1QL syntax for dropping a primary with a name is actually similar to dropping secondary index
        return new DefaultDropPath().drop(keyspace, customPrimaryName);
    }
}
