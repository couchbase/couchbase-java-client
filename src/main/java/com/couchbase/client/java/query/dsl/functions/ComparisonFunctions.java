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
    private ComparisonFunctions() {}

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