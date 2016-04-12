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
import com.couchbase.client.java.query.dsl.Expression;

@InterfaceStability.Experimental
@InterfaceAudience.Private
public class KeysElement implements Element {

    private final Expression expression;

    private final ClauseType clauseType;

    public KeysElement(ClauseType clauseType, Expression expression) {
        this.clauseType = clauseType;
        this.expression = expression;
    }

    @Override
    public String export() {
        return clauseType.n1ql + expression.toString();
    }

    public static enum ClauseType {

        //Note: as of N1QL Beta, Colm confirmed that the PRIMARY prefix for KEYS was just eye candy / alternative syntax
        // and not carrying any semantic, so it hasn't been represented here.

        /** the clause type for setting keys to use in a join / nest / unnest clause **/
        JOIN_ON("ON KEYS "),
        /** the clause type for selecting by primary key in a from clause **/
        USE_KEYSPACE("USE KEYS ");

        private final String n1ql;

        ClauseType(String n1ql) {
            this.n1ql = n1ql;
        }

    }
}
