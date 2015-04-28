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
