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
