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
import com.couchbase.client.java.query.dsl.element.IndexNamesElement;
import com.couchbase.client.java.query.dsl.path.AbstractPath;

/**
 * See {@link IndexNamesPath}.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Private
public class DefaultIndexNamesPath extends AbstractPath implements IndexNamesPath {

    protected DefaultIndexNamesPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public UsingPath indexes(String indexName, String... indexNames) {
        element(new IndexNamesElement(indexName, indexNames));
        return new DefaultUsingPath(this);
    }

    @Override
    public UsingPath indexes(List<String> indexNames) {
        if (indexNames.isEmpty()) {
            throw new IllegalArgumentException("indexNames must have at least one name");
        }
        String first = indexNames.get(0);
        if (indexNames.size() > 1) {
            String[] others = indexNames.subList(1, indexNames.size()).toArray(new String[indexNames.size() - 1]);
            element(new IndexNamesElement(first, others));
        } else {
            element(new IndexNamesElement(first));
        }
        return new DefaultUsingPath(this);
    }

    @Override
    public UsingPath primary() {
        element(new IndexNamesElement(Index.PRIMARY_NAME));
        return new DefaultUsingPath(this);
    }
}
