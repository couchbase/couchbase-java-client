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
 * DSL for N1QL functions in the Comparison category.
 *
 * Comparison functions determine the greatest or least value from a set of values.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class ComparisonFunctions {
    /**
     * Returned expression results in the largest non-NULL, non-MISSING value if the values are
     * of the same type, otherwise NULL. At least two expressions are necessary.
     */
    public static Expression greatest(Expression e1, Expression e2, Expression... otherExpressions) {
        StringBuilder greatest = new StringBuilder("GREATEST(");
        greatest.append(e1.toString()).append(", ").append(e2.toString());
        if (otherExpressions == null) {
            return x(greatest.append(')').toString());
        }

        for (Expression otherExpression : otherExpressions) {
            if (otherExpression == null) {
                otherExpression = Expression.NULL();
            }
            greatest.append(", ").append(otherExpression.toString());
        }
        greatest.append(')');
        return x(greatest.toString());
    }

    /**
     * Returned expression results in the smallest non-NULL, non-MISSING value
     * if the values are of the same type, otherwise NULL. At least two expressions are necessary.
     */
    public static Expression least(Expression e1, Expression e2, Expression... otherExpressions) {
        StringBuilder least = new StringBuilder("LEAST(");
        least.append(e1.toString()).append(", ").append(e2.toString());
        if (otherExpressions == null) {
            return x(least.append(')').toString());
        }

        for (Expression otherExpression : otherExpressions) {
            if (otherExpression == null) {
                otherExpression = Expression.NULL();
            }
            least.append(", ").append(otherExpression.toString());
        }
        least.append(')');
        return x(least.toString());
    }
}