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
