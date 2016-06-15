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
package com.couchbase.client.java.query.dsl.functions;

import static com.couchbase.client.java.query.dsl.Expression.x;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.query.dsl.Expression;

/**
 * DSL for N1QL functions in the aggregate category.
 *
 * Aggregate functions take multiple values from documents, perform calculations, and return a single
 * value as the result.
 *
 * You can only use aggregate functions in SELECT, LETTING, HAVING, and ORDER BY clauses.
 * When using an aggregate function in a query, the query operates as an aggregate query.
 *
 * See N1QL reference documentation: http://docs.couchbase.com/4.0/n1ql/n1ql-language-reference/aggregatefun.html
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class AggregateFunctions {

    private AggregateFunctions() {}

    /**
     * Returned expression results in a array of the non-MISSING values in the group, including NULLs.
     */
    public static Expression arrayAgg(Expression expression) {
        return x("ARRAY_AGG(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in a array of the non-MISSING values in the group, including NULLs.
     */
    public static Expression arrayAgg(String expression) {
        return arrayAgg(x(expression));
    }


    /**
     * Returned expression results in the arithmetic mean (average) of all the distinct number values in the group.
     */
    public static Expression avg(Expression expression) {
        return x("AVG(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the arithmetic mean (average) of all the distinct number values in the group.
     */
    public static Expression avg(String expression) {
        return avg(x(expression));
    }

    /**
     * Returned expression results in count of all the non-NULL and non-MISSING values in the group.
     */
    public static Expression count(Expression expression) {
        return x("COUNT(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in count of all the non-NULL and non-MISSING values in the group
     */
    public static Expression count(String expression) {
        return count(x(expression));
    }

    /**
     * Returned expression results in a count of all the input rows for the group, regardless of value (including NULL).
     */
    public static Expression countAll() {
        return x("COUNT(*)");
    }

    /**
     * Returned expression results in the maximum non-NULL, non-MISSING value in the group in N1QL collation order.
     */
    public static Expression max(Expression expression) {
        return x("MAX(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the maximum non-NULL, non-MISSING value in the group in N1QL collation order.
     */
    public static Expression max(String expression) {
        return max(x(expression));
    }

    /**
     * Returned expression results in the minimum non-NULL, non-MISSING value in the group in N1QL collation order.
     */
    public static Expression min(Expression expression) {
        return x("MIN(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the minimum non-NULL, non-MISSING value in the group in N1QL collation order.
     */
    public static Expression min(String expression) {
        return min(x(expression));
    }

    /**
     * Returned expression results in the sum of all the number values in the group.
     */
    public static Expression sum(Expression expression) {
        return x("SUM(" + expression.toString() + ")");
    }

    public static Expression sum(String expression) {
        return sum(x(expression));
    }

    /**
     * prefixes an expression with DISTINCT, useful for example for distinct count "COUNT(DISTINCT expression)".
     */
    public static Expression distinct(Expression expression) {
        return x("DISTINCT " + expression.toString());
    }

    /**
     * prefixes an expression with DISTINCT, useful for example for distinct count "COUNT(DISTINCT expression)".
     */
    public static Expression distinct(String expression) {
        return x("DISTINCT " + expression);
    }


}
