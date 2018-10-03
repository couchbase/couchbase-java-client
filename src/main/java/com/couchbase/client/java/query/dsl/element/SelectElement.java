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
import com.couchbase.client.java.query.dsl.path.SelectType;

@InterfaceStability.Experimental
@InterfaceAudience.Private
public class SelectElement implements Element {

    private final SelectType selectType;
    private final Expression[] expressions;


    public SelectElement(SelectType selectType, Expression... expressions) {
        this.selectType = selectType;
        this.expressions = expressions;
    }

    @Override
    public String export() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        if (selectType != SelectType.DEFAULT) {
            sb.append(selectType.value()).append(" ");
        }
        for (int i=0; i < expressions.length; i++) {
            sb.append(expressions[i].toString());
            if (i < expressions.length-1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
