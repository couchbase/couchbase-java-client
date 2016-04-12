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
