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

/**
 * Element of the Index DSL to create various forms of indexes.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Private
public class IndexElement implements Element {

    private final String name;
    private final boolean primary;

    public IndexElement(String indexName, boolean forcePrimary) {
        this.name = indexName;
        this.primary = forcePrimary;
    }

    public IndexElement(String indexName) {
        this(indexName, false);
    }

    public IndexElement() {
        this(null, true);
    }

    @Override
    public String export() {
        if (primary && name == null) {
            return "CREATE PRIMARY INDEX";
        } else if (primary) {
            return "CREATE PRIMARY INDEX `" + name + "`";
        } else {
            return "CREATE INDEX `" + name + "`";
        }
    }
}
