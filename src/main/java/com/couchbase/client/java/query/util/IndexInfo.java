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
package com.couchbase.client.java.query.util;

import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.Index;
import com.couchbase.client.java.query.dsl.path.index.IndexType;

/**
 * Contains meta-information about a N1QL index.
 *
 * @author Simon Baslé
 * @since 2.2
 */
public class IndexInfo {

    public static final String PRIMARY_DEFAULT_NAME = Index.PRIMARY_NAME;

    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(IndexInfo.class);

    private final String name;
    private final boolean isPrimary;
    private final IndexType type;
    private final String rawType;
    private final String state;
    private final String keyspace;
    private final String namespace;
    private final JsonArray indexKey;
    private final String condition;

    private final JsonObject raw;

    public IndexInfo(JsonObject raw) {
        this.raw = raw;

        this.name = raw.getString("name");
        this.state = raw.getString("state");
        this.keyspace = raw.getString("keyspace_id");
        this.namespace = raw.getString("namespace_id");
        this.indexKey = raw.getArray("index_key");

        String rawCondition = raw.getString("condition");
        this.condition = rawCondition == null ? "" : rawCondition;

        this.rawType = raw.getString("using");
        if (rawType == null) {
            this.type = IndexType.GSI; //assume GSI
        } else {
            //let's avoid unrecognized type crashing the constructor
            IndexType candidateType;
            try {
                candidateType = IndexType.valueOf(rawType.toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Unknown index type %s for index %s", rawType, name);
                candidateType = null;
            }
            this.type = candidateType;
        }

        this.isPrimary = raw.getBoolean("is_primary") == Boolean.TRUE;
    }

    public boolean isPrimary() {
        return this.isPrimary;
    }

    public String name() {
        return this.name;
    }

    /**
     * @return the {@link IndexType} enum corresponding to this index, or null if the type couldn't be parsed, in which
     * case {@link #rawType()} can be used to see which type string is advertised by the query service.
     */
    public IndexType type() {
        return type;
    }

    /**
     * @return the raw type string advertised by the query service for this index. Can be useful for unknown types that
     * couldn't be parsed into a {@link IndexType} for {@link #type()}.
     */
    public String rawType() {
        return rawType;
    }

    public String state() {
        return state;
    }

    /**
     * @return the keyspace for the index, typically the bucket name.
     */
    public String keyspace() {
        return keyspace;
    }

    /**
     * @return the namespace for the index. A namespace is a resource pool that contains multiple keyspaces.
     * @see #keyspace()
     */
    public String namespace() {
        return namespace;
    }

    /**
     * Return an {@link JsonArray array} of Strings that represent the index key(s).
     * The array is empty in the case of a PRIMARY INDEX.
     * Note that the query service can present the key in a slightly different manner from when you declared the index:
     * for instance, it will show the indexed fields in an escaped format (surrounded by backticks).
     *
     * @return an array of Strings that represent the index key(s), or an empty array in the case of a PRIMARY index.
     */
    public JsonArray indexKey() {
        return this.indexKey;
    }

    /**
     * Return the {@link String} representation of the index's condition (the WHERE clause of the index), or an empty
     * String if no condition was set.
     *
     * Note that the query service can present the condition in a slightly different manner from when you declared the index:
     * for instance it will wrap expressions with parentheses and show the fields in an escaped format (surrounded by
     * backticks).
     *
     * @return the condition/WHERE clause of the index or empty string if none.
     */
    public String condition() {
        return this.condition;
    }

    /**
     * @return the raw JSON representation of the index information, as returned by the query service.
     */
    public JsonObject raw() {
        return raw;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IndexInfo indexInfo = (IndexInfo) o;

        if (isPrimary != indexInfo.isPrimary) {
            return false;
        }
        if (!name.equals(indexInfo.name)) {
            return false;
        }
        if (!rawType.equals(indexInfo.rawType)) {
            return false;
        }
        if (!state.equals(indexInfo.state)) {
            return false;
        }
        if (!keyspace.equals(indexInfo.keyspace)) {
            return false;
        }
        if (!namespace.equals(indexInfo.namespace)) {
            return false;
        }
        if (!condition.equals(indexInfo.condition)) {
            return false;
        }
        return indexKey.equals(indexInfo.indexKey);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (isPrimary ? 1 : 0);
        result = 31 * result + rawType.hashCode();
        result = 31 * result + state.hashCode();
        result = 31 * result + keyspace.hashCode();
        result = 31 * result + namespace.hashCode();
        result = 31 * result + indexKey.hashCode();
        result = 31 * result + condition.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return raw.toString();
    }
}
