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
