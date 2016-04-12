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
package com.couchbase.client.java.query.dsl.element;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.query.dsl.path.index.IndexReference;

/**
 * Element of the Index DSL to create various forms of indexes.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Private
public class HintIndexElement implements Element {

    private final IndexReference[] indexReferences;

    public HintIndexElement(IndexReference... indexReferences) {
        this.indexReferences = indexReferences;
    }

    @Override
    public String export() {
        if (indexReferences == null || indexReferences.length < 1) {
            return "";
        }
        StringBuilder n1ql = new StringBuilder("USE INDEX (");
        for (IndexReference indexReference : indexReferences) {
            n1ql.append(indexReference.toString()).append(',');
        }
        n1ql.deleteCharAt(n1ql.length() - 1);
        n1ql.append(')');

        return n1ql.toString();
    }
}
