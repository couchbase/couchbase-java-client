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

/**
 * Element of the Index DSL that targets the keyspace on which to apply an index.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Private
public class OnElement implements Element {

    private final String fullKeyspace;
    private final Expression expression;
    private final Expression[] additionalExpressions;

    public OnElement(String namespace, String keyspace, Expression expression,
            Expression[] additionalExpressions) {
        this.fullKeyspace = (namespace == null) ?
                "`" + keyspace + "`" :
                "`" + namespace + "`:`" + keyspace + "`";
        this.expression = expression;
        this.additionalExpressions = additionalExpressions;
    }

    @Override
    public String export() {
        if (expression == null) {
            return "ON " + fullKeyspace;
        } else {
            if (additionalExpressions == null || additionalExpressions.length == 0) {
                return "ON " + fullKeyspace + "(" + expression.toString() + ")";
            } else {
                StringBuilder on = new StringBuilder("ON ");
                on.append(fullKeyspace).append('(').append(expression);
                for (Expression additionalExpression : additionalExpressions) {
                    on.append(", ").append(additionalExpression.toString());
                }
                on.append(')');
                return on.toString();
            }
        }
    }
}
