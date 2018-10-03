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


import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.SelectElement;

import static com.couchbase.client.java.query.dsl.Expression.x;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DefaultSelectPath extends AbstractPath implements SelectPath {

    public DefaultSelectPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public FromPath select(Expression... expressions) {
        element(new SelectElement(SelectType.DEFAULT, expressions));
        return new DefaultFromPath(this);
    }

    @Override
    public FromPath selectAll(Expression... expressions) {
        element(new SelectElement(SelectType.ALL, expressions));
        return new DefaultFromPath(this);
    }

    @Override
    public FromPath selectDistinct(Expression... expressions) {
        element(new SelectElement(SelectType.DISTINCT, expressions));
        return new DefaultFromPath(this);
    }

    @Override
    public FromPath selectRaw(Expression expression) {
        element(new SelectElement(SelectType.RAW, expression));
        return new DefaultFromPath(this);
    }

    @Override
    public FromPath selectDistinctRaw(Expression expression) {
        element(new SelectElement(SelectType.DISTINCT_RAW, expression));
        return new DefaultFromPath(this);
    }

    @Override
    public FromPath select(String... expressions) {
        Expression[] converted = new Expression[expressions.length];
        for (int i = 0; i < expressions.length; i++) {
            converted[i] = x(expressions[i]);
        }
        return select(converted);
    }

    @Override
    public FromPath selectAll(String... expressions) {
        Expression[] converted = new Expression[expressions.length];
        for (int i = 0; i < expressions.length; i++) {
            converted[i] = x(expressions[i]);
        }
        return selectAll(converted);
    }

    @Override
    public FromPath selectDistinct(String... expressions) {
        Expression[] converted = new Expression[expressions.length];
        for (int i = 0; i < expressions.length; i++) {
            converted[i] = x(expressions[i]);
        }
        return selectDistinct(converted);
    }

    @Override
    public FromPath selectRaw(String expression) {
        return selectRaw(x(expression));
    }

    @Override
    public FromPath selectDistinctRaw(String expression) {
        return selectDistinctRaw(x(expression));
    }
}
