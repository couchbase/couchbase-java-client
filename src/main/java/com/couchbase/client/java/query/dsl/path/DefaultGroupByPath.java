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

import static com.couchbase.client.java.query.dsl.Expression.x;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.GroupByElement;

/**
 * .
 *
 * @author Michael Nitschinger
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class DefaultGroupByPath extends DefaultSelectResultPath implements GroupByPath {

    public DefaultGroupByPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public LettingPath groupBy(Expression... expressions) {
        element(new GroupByElement(expressions));
        return new DefaultLettingPath(this);
    }

    @Override
    public LettingPath groupBy(String... identifiers) {
        Expression[] expressions = new Expression[identifiers.length];
        for (int i = 0; i < identifiers.length; i++) {
            expressions[i] = x(identifiers[i]);
        }
        return groupBy(expressions);
    }
}
