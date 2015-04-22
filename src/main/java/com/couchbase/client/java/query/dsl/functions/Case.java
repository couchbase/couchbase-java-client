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

import java.util.ArrayList;
import java.util.List;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.query.dsl.Expression;

/**
 * DSL for creating CASE...END constructs.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class Case {

    /**
     * Constructs a "simple case" expression. Initial caseExpression will be compared to each WHEN clause for equality,
     * and if it matches the corresponding THEN expression will be returned. If no WHEN-THEN matches, the ELSE expression
     * is returned. If no ELSE was provided, NULL is returned.
     *
     * @param expected the initial caseExpression on which to match.
     */
    public static WhenClause caseSimple(Expression expected) {
        return new CaseBuilder(expected);
    }

    /**
     * Constructs a "search case" expression. Each WHEN clause will have its condition inspected in turn, and if it
     * holds true the corresponding THEN expression will be returned. If no WHEN-THEN matches, the ELSE expression
     * is returned. If no ELSE was provided, NULL is returned.
     */
    public static WhenClause caseSearch() {
        return new CaseBuilder(null);
    }

    //==== INTERFACES FOR EACH SECTION ====

    public interface CaseClause extends WhenClause {
        /** ends the CASE without an ELSE clause */
        Expression end();

        /** ends the CASE, adding a default return expression as an ELSE clause */
        Expression elseReturn(Expression elseResult);
    }

    public interface WhenClause {
        /**
         * WHEN clause, to be followed by its {@link ThenClause#then(Expression) THEN clause}.
         *
         * If the CASE is a "search case", the given {@link Expression} must be a condition.
         * Otherwise, in a "simple case", the given Expression is matched against the initial case Expression for equality.
         *
         * If the condition holds true, the CASE will return the expression given by the following THEN clause.
         * Otherwise next WHEN clause is evaluated, if note ELSE clause is used, if none the CASE returns NULL.
         *
         * @see Case#caseSearch()
         * @see Case#caseSimple(Expression)
         */
        ThenClause when(Expression conditionOrExpression);
    }

    public interface ThenClause {
        /**
         * THEN clause associated with a {@link WhenClause#when(Expression) WHEN clause}.
         *
         * This describes the result of the CASE if the corresponding WHEN clause matched (ie either it was a condition
         * holding true or an {@link Expression} equal to the simple case's caseExpression).
         */
        CaseClause then(Expression expression);
    }

    //==== THE BUILDER THAT MAKES IT COME TOGETHER ====

    private static final class CaseBuilder implements CaseClause, ThenClause {

        private int count = 0;
        private final List<Expression> whens;
        private final List<Expression> thens;
        private Expression elseResult = null;
        private final Expression caseExpression;

        private CaseBuilder(Expression caseExpression) {
            this.caseExpression = caseExpression;
            this.whens = new ArrayList<Expression>(1);
            this.thens = new ArrayList<Expression>(1);
        }

        @Override
        public ThenClause when(Expression conditionOrExpression) {
            whens.add(conditionOrExpression);
            thens.add(Expression.NULL());
            return this;
        }

        @Override
        public CaseClause then(Expression expression) {
            thens.set(count, expression);
            count++;
            return this;
        }

        @Override
        public Expression elseReturn(Expression elseResult) {
            this.elseResult = elseResult;
            return end();
        }

        @Override
        public Expression end() {
            StringBuilder result = new StringBuilder("CASE ");
            if (caseExpression != null) {
                result.append(caseExpression.toString()).append(' ');
            }
            for (int i = 0; i < count; i++) {
                result.append("WHEN ")
                    .append(whens.get(i))
                    .append(" THEN ")
                    .append(thens.get(i))
                    .append(", ");
            }
            result.delete(result.length() - 2, result.length());
            if (elseResult != null) {
                result.append(" ELSE ")
                        .append(elseResult.toString());
            }
            result.append(" END");
            return x(result.toString());
        }
    }
}
