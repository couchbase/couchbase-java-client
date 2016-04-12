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
 * Element for the initial clause of a DROP index statement.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Private
public class DropIndexElement implements Element {

    private final String fullKeyspace;
    private final String indexName;

    public DropIndexElement(String namespace, String keyspace, String indexName) {
        if (namespace == null) {
            this.fullKeyspace = ESCAPE_CHAR + keyspace + ESCAPE_CHAR;
        } else {
            this.fullKeyspace = ESCAPE_CHAR + namespace + "`:`" + keyspace + ESCAPE_CHAR;
        }
        this.indexName = indexName == null ? null : ESCAPE_CHAR + indexName + ESCAPE_CHAR;
    }

    @Override
    public String export() {
        if (indexName == null) {
            return "DROP PRIMARY INDEX ON " + fullKeyspace;
        }
        return "DROP INDEX " + fullKeyspace + "." + indexName;
    }
}
