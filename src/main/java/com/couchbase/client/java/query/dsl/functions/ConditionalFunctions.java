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
 * DSL for N1QL Conditional functions (for unknowns and numbers).
 *
 * Conditional functions for unknowns evaluate expressions to determine if the values and
 * formulas meet the specified condition.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class ConditionalFunctions {

    private ConditionalFunctions() {}

    protected static Expression build(String operator, Expression expression1, Expression expression2,
            Expression... others) {
        StringBuilder result = new StringBuilder(operator);
        result.append('(').append(expression1.toString()).append(", ").append(expression2.toString());
        if (others != null) {
            for (Expression other : others) {
                if (other == null) {
                    other = Expression.NULL();
                }
                result.append(", ").append(other.toString());
            }
        }
        result.append(')');
        return x(result.toString());
    }

    //===== FOR UNKNOWNS =====

    /**
     * Returned expression results in the first non-MISSING value.
     */
    public static Expression ifMissing(Expression expression1, Expression expression2, Expression... others) {
        return build("IFMISSING", expression1, expression2, others);
    }

    /**
     * Returned expression results in first non-NULL, non-MISSING value.
     */
    public static Expression ifMissingOrNull(Expression expression1, Expression expression2, Expression... others) {
        return build("IFMISSINGORNULL", expression1, expression2, others);
    }

    /**
     * Returned expression results in first non-NULL value.
     * Note that this function might return MISSING if there is no non-NULL value.
     */
    public static Expression ifNull(Expression expression1, Expression expression2, Expression... others) {
        return build("IFNULL", expression1, expression2, others);
    }

    /**
     * Returned expression results in MISSING if expression1 = expression2, otherwise returns expression1.
     * Returns MISSING or NULL if either input is MISSING or NULL..
     */
    public static Expression missingIf(Expression expression1, Expression expression2) {
        return x("MISSINGIF(" + expression1.toString() + ", " + expression2.toString() + ")");
    }

    /**
     * Returned expression results in NULL if expression1 = expression2, otherwise returns expression1.
     * Returns MISSING or NULL if either input is MISSING or NULL..
     */
    public static Expression nullIf(Expression expression1, Expression expression2) {
        return x("NULLIF(" + expression1.toString() + ", " + expression2.toString() + ")");
    }

    //===== FOR NUMBERS =====
    /**
     * Returned expression results in first non-MISSING, non-Inf number.
     * Returns MISSING or NULL if a non-number input is encountered first.
     */
    public static Expression ifInf(Expression expression1, Expression expression2, Expression... others) {
        return build("IFINF", expression1, expression2, others);
    }

    /**
     * Returned expression results in first non-MISSING, non-NaN number.
     * Returns MISSING or NULL if a non-number input is encountered first
     */
    public static Expression ifNaN(Expression expression1, Expression expression2, Expression... others) {
        return build("IFNAN", expression1, expression2, others);
    }

    /**
     * Returned expression results in first non-MISSING, non-Inf, or non-NaN number.
     * Returns MISSING or NULL if a non-number input is encountered first.
     */
    public static Expression ifNaNOrInf(Expression expression1, Expression expression2, Expression... others) {
        return build("IFNANORINF", expression1, expression2, others);
    }

    /**
     * Returned expression results in NaN if expression1 = expression2, otherwise returns expression1.
     * Returns MISSING or NULL if either input is MISSING or NULL.
     */
    public static Expression nanIf(Expression expression1, Expression expression2) {
        return x("NANIF(" + expression1.toString() + ", " + expression2.toString() + ")");
    }

    /**
     * Returned expression results in NegInf if expression1 = expression2, otherwise returns expression1.
     * Returns MISSING or NULL if either input is MISSING or NULL.
     */
    public static Expression negInfIf(Expression expression1, Expression expression2) {
        return x("NEGINFIF(" + expression1.toString() + ", " + expression2.toString() + ")");
    }

    /**
     * Returned expression results in PosInf if expression1 = expression2, otherwise returns expression1.
     * Returns MISSING or NULL if either input is MISSING or NULL.
     */
    public static Expression posInfIf(Expression expression1, Expression expression2) {
        return x("POSINFIF(" + expression1.toString() + ", " + expression2.toString() + ")");
    }
}
