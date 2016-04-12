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
package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.query.dsl.element.HintIndexElement;
import com.couchbase.client.java.query.dsl.path.index.IndexReference;

/**
 * See {@link HintPath}.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class DefaultHintPath extends DefaultKeysPath implements HintPath {

    public DefaultHintPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public KeysPath useIndex(IndexReference... indexes) {
        element(new HintIndexElement(indexes));
        return new DefaultKeysPath(this);
    }

    @Override
    public KeysPath useIndex(String... indexes) {
        IndexReference[] indexRefs = new IndexReference[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            indexRefs[i] = IndexReference.indexRef(indexes[i]);
        }
        return useIndex(indexRefs);
    }
}
