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
package com.couchbase.client.java.query.dsl;

import static com.couchbase.client.java.query.dsl.Expression.x;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class Sort {

    private final Expression expression;
    private final Order ordering;

    private Sort(final Expression expression, final Order ordering) {
        this.expression = expression;
        this.ordering = ordering;
    }

    /**
     * Use default sort, don't specify an order in the resulting expression.
     */
    public static Sort def(final Expression expression) {
        return new Sort(expression, null);
    }

    /**
     * Use default sort, don't specify an order in the resulting expression.
     */
    public static Sort def(final String expression) {
        return def(x(expression));
    }

    public static Sort desc(final Expression expression) {
        return new Sort(expression, Order.DESC);
    }

    public static Sort desc(final String expression) {
        return desc(x(expression));
    }

    public static Sort asc(final Expression expression) {
        return new Sort(expression, Order.ASC);
    }

    public static Sort asc(final String expression) {
        return asc(x(expression));
    }

    @Override
    public String toString() {
        if (ordering != null) {
            return expression.toString() + " " + ordering;
        } else {
            return expression.toString();
        }
    }

    public static enum Order  {
        ASC,
        DESC
    }
}
