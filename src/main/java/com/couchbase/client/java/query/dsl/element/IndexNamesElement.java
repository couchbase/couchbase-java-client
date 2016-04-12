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
 * Element for listing index names when Building an Index.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Private
public class IndexNamesElement implements Element {

    private final String indexName;
    private final String[] otherNames;

    public IndexNamesElement(String indexName, String... indexNames) {
        this.indexName = indexName;
        this.otherNames = indexNames;
    }

    @Override
    public String export() {
        StringBuilder sb = new StringBuilder("(`").append(indexName).append('`');
        for (String otherName : otherNames) {
            sb.append(", `").append(otherName).append('`');
        }
        sb.append(')');
        return sb.toString();
    }
}
