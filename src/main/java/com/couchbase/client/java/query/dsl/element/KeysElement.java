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
