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
package com.couchbase.client.java.query.dsl.path.index;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.OnElement;
import com.couchbase.client.java.query.dsl.path.AbstractPath;

/**
 * See {@link OnPath}.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Private
public class DefaultOnPath extends AbstractPath implements OnPath {

    public DefaultOnPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public WherePath on(String namespace, String keyspace, Expression expression, Expression... additionalExpressions) {
        element(new OnElement(namespace, keyspace, expression, additionalExpressions));
        return new DefaultWherePath(this);
    }

    @Override
    public WherePath on(String keyspace, Expression expression, Expression... additionalExpressions) {
        return on(null, keyspace, expression, additionalExpressions);
    }
}
