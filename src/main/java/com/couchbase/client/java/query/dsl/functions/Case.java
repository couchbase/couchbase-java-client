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
         * Otherwise next WHEN clause is evaluated, if none left the ELSE clause is used, if none the CASE returns NULL.
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
                    .append(' ');
            }
            result.delete(result.length() - 1, result.length());
            if (elseResult != null) {
                result.append(" ELSE ")
                        .append(elseResult.toString());
            }
            result.append(" END");
            return x(result.toString());
        }
    }
}
