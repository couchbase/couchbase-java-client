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
import com.couchbase.client.java.query.Index;

/**
 * {@link IndexReference} wraps an index name and an index type (with the `USING GSI|VIEW` syntax).
 *
 * @author Simon Baslé
 * @since 2.2
 * @see #indexRef(String)
 * @see #indexRef(String, IndexType)
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class IndexReference {

    private String indexReference;

    private IndexReference(String representation) {
        this.indexReference = representation;
    }

    @Override
    public String toString() {
        return indexReference;
    }

    /**
     * Constructs an {@link IndexReference} given an index name (which will be escaped). No USING clause
     * is set explicitely.
     */
    public static final IndexReference indexRef(String indexName) {
        return indexRef(indexName, null);
    }

    /**
     * Constructs an {@link IndexReference} given an index name (which will be escaped) and an explicit
     * {@link IndexType} to use in a USING clause.
     */
    public static final IndexReference indexRef(String indexName, IndexType type) {
        if (type == null) {
            return new IndexReference("`" + indexName + "`");
        }
        return new IndexReference("`" + indexName + "` USING " + type.toString());
    }
}
