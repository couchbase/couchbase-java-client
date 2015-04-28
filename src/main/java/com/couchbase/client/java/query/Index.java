/**
 * Copyright (C) 2015 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */
package com.couchbase.client.java.query;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.query.dsl.element.BuildIndexElement;
import com.couchbase.client.java.query.dsl.path.index.BuildIndexPath;
import com.couchbase.client.java.query.dsl.path.index.DefaultBuildIndexPath;
import com.couchbase.client.java.query.dsl.path.index.DefaultCreateIndexPath;
import com.couchbase.client.java.query.dsl.path.index.DefaultDropPath;
import com.couchbase.client.java.query.dsl.path.index.IndexType;
import com.couchbase.client.java.query.dsl.path.index.OnPath;
import com.couchbase.client.java.query.dsl.path.index.OnPrimaryPath;
import com.couchbase.client.java.query.dsl.path.index.UsingPath;
import com.couchbase.client.java.query.dsl.path.index.UsingWithPath;

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
     */
    public static UsingPath dropPrimaryIndex(String namespace, String keyspace) {
        return new DefaultDropPath().dropPrimary(namespace, keyspace);
    }

    /**
     * Drop the primary index in the given keyspace.
     *
     * @param keyspace the keyspace (bucket, will be escaped).
     */
    public static UsingPath dropPrimaryIndex(String keyspace) {
        return new DefaultDropPath().dropPrimary(keyspace);
    }
}
