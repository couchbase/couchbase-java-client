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

/**
 * Filters resulting rows based on the given expression.
 *
 * The where condition is evaluated for each resulting row, and only rows evaluating true are retained. All
 * method overloads which do not take an {@link Expression} will be converted to one internally.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public interface WherePath extends GroupByPath {

    /**
     * Filter resulting rows based on the given expression.
     *
     * @param expression the filter expression.
     * @return the next possible steps.
     */
    GroupByPath where(Expression expression);

    /**
     * Filter resulting rows based on the given expression.
     *
     * The given string will be converted into an expression internally.
     *
     * @param expression the filter expression.
     * @return the next possible steps.
     */
    GroupByPath where(String expression);

}
