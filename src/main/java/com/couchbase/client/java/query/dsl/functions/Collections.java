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
 * DSL for N1QL Collections (aka comprehensions, ANY, EVERY, ARRAY and FIRST...).
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class Collections {

    private Collections() {}

    private abstract static class CollectionBuilder {
        private Expression prefix;
        private List<Expression> variables;

        private CollectionBuilder(Expression prefix, String firstVar, Expression firstExpr, boolean useIn) {
            this.prefix = prefix;
            this.variables = new ArrayList<Expression>(2);
            if (useIn) {
                in(firstVar, firstExpr);
            } else {
                within(firstVar, firstExpr);
            }
        }

        /**
         * Add an in-expression to the clause (a variable name and its associated expression)
         */
        protected CollectionBuilder in(String variable, Expression expression) {
            variables.add(x(variable + " IN " + expression.toString()));
            return this;
        }

        /**
         * Add a within-expression to the clause (a variable name and its associated expression)
         */
        protected CollectionBuilder within(String variable, Expression expression) {
            variables.add(x(variable + " WITHIN " + expression.toString()));
            return this;
        }

        /**
         * Ends the comprehension without specifying any condition
         */
        public final Expression end() {
            return end(null, null);
        }

        protected final Expression end(String conditionKeyword, Expression condition) {
            StringBuilder sb = new StringBuilder(prefix.toString()).append(' ');
            for (Expression variable : variables) {
                sb.append(variable).append(", ");
            }
            if (!variables.isEmpty()) {
                sb.delete(sb.length() - 2, sb.length());
            }
            if (condition != null && conditionKeyword != null) {
                sb.append(' ').append(conditionKeyword.trim()).append(' ');
                sb.append(condition.toString());
            }
            sb.append(" END");
            return x(sb.toString());
        }

    }

    public static final class SatisfiesBuilder extends CollectionBuilder {

        private SatisfiesBuilder(Expression prefix, String firstVar, Expression firstExpr, boolean useIn) {
            super(prefix, firstVar, firstExpr, useIn);
        }

        @Override
        public SatisfiesBuilder in(String variable, Expression expression) {
            return (SatisfiesBuilder) super.in(variable, expression);
        }

        @Override
        public SatisfiesBuilder within(String variable, Expression expression) {
            return (SatisfiesBuilder) super.within(variable, expression);
        }

        /**
         * Sometimes the conditions you want to filter need to be applied to the arrays nested inside the document.
         * The SATISFIES keyword is used to specify the filter condition. This method also ends the comprehension.
         *
         * See {@link #end()} to avoid setting any condition, instead just ending the comprehension.
         */
        public Expression satisfies(Expression condition) {
            return super.end("SATISFIES", condition);
        }
    }

    public static final class WhenBuilder extends CollectionBuilder {

        private WhenBuilder(Expression prefix, String firstVar, Expression firstExpr, boolean useIn) {
            super(prefix, firstVar, firstExpr, useIn);
        }

        @Override
        public WhenBuilder in(String variable, Expression expression) {
            return (WhenBuilder) super.in(variable, expression);
        }

        @Override
        public WhenBuilder within(String variable, Expression expression) {
            return (WhenBuilder) super.within(variable, expression);
        }

        /**
         * Set a WHEN clause, a condition that must be satisfied by the array comprehension,
         * and ends the comprehension. See {@link #end()} to avoid setting any condition.
         */
        public Expression when(Expression condition) {
            return super.end("WHEN", condition);
        }
    }

    /**
     * Create an ANY comprehension with a first IN range.
     *
     * ANY is a range predicate that allows you to test a Boolean condition over the
     * elements or attributes of a collection, object, or objects. It uses the IN and WITHIN operators to range through
     * the collection. IN ranges in the direct elements of its array expression, WITHIN also ranges in its descendants.
     *
     * If at least one item in the array satisfies the ANY expression, then ANY returns TRUE, otherwise returns FALSE.
     */
    public static SatisfiesBuilder anyIn(String variable, Expression expression) {
        return new SatisfiesBuilder(x("ANY"), variable, expression, true);
    }

    /**
     * Create an ANY AND EVERY comprehension with a first IN range.
     *
     * ANY is a range predicate that allows you to test a Boolean condition over the
     * elements or attributes of a collection, object, or objects. It uses the IN and WITHIN operators to range through
     * the collection.
     *
     * EVERY is a range predicate that allows you to test a Boolean condition over the elements or attributes of a
     * collection, object, or objects. It uses the IN and WITHIN operators to range through the collection.
     *
     */
    public static SatisfiesBuilder anyAndEveryIn(String variable, Expression expression) {
        return new SatisfiesBuilder(x("ANY AND EVERY"), variable, expression, true);
    }

    /**
     * Create an ANY comprehension with a first WITHIN range.
     *
     * ANY is a range predicate that allows you to test a Boolean condition over the
     * elements or attributes of a collection, object, or objects. It uses the IN and WITHIN operators to range through
     * the collection.
     * IN ranges in the direct elements of its array expression, WITHIN also ranges in its descendants.
     *
     * If at least one item in the array satisfies the ANY expression, then ANY returns TRUE, otherwise returns FALSE.
     */
    public static SatisfiesBuilder anyWithin(String variable, Expression expression) {
        return new SatisfiesBuilder(x("ANY"), variable, expression, false);
    }

    /**
     * Create an EVERY comprehension with a first IN range.
     *
     * EVERY is a range predicate that allows you to test a Boolean condition over the elements or attributes of a
     * collection, object, or objects. It uses the IN and WITHIN operators to range through the collection.

     * IN ranges in the direct elements of its array expression, WITHIN also ranges in its descendants.
     *
     * If every array element satisfies the EVERY expression, it returns TRUE. Otherwise it returns FALSE.
     * If the array is empty, it returns TRUE.
     */
    public static SatisfiesBuilder everyIn(String variable, Expression expression) {
        return new SatisfiesBuilder(x("EVERY"), variable, expression, true);
    }

    /**
     * Create an EVERY comprehension with a first WITHIN range.
     *
     * EVERY is a range predicate that allows you to test a Boolean condition over the elements or attributes of a
     * collection, object, or objects. It uses the IN and WITHIN operators to range through the collection.

     * IN ranges in the direct elements of its array expression, WITHIN also ranges in its descendants.
     *
     * If every array element satisfies the EVERY expression, it returns TRUE. Otherwise it returns FALSE.
     * If the array is empty, it returns TRUE.
     */
    public static SatisfiesBuilder everyWithin(String variable, Expression expression) {
        return new SatisfiesBuilder(x("EVERY"), variable, expression, false);
    }

    /**
     * Create an ARRAY comprehension with a first IN range.
     *
     * The ARRAY operator lets you map and filter the elements or attributes of a collection, object, or objects.
     * It evaluates to an array of the operand expression, that satisfies the WHEN clause, if provided.
     *
     * For elements, IN ranges in the direct elements of its array expression, WITHIN also ranges in its descendants.
     */
    public static WhenBuilder arrayIn(Expression arrayExpression, String variable, Expression expression) {
        return new WhenBuilder(x("ARRAY " + arrayExpression.toString() + " FOR"),
                variable, expression, true);
    }

    /**
     * Create an ARRAY comprehension with a first WITHIN range.
     *
     * The ARRAY operator lets you map and filter the elements or attributes of a collection, object, or objects.
     * It evaluates to an array of the operand expression, that satisfies the WHEN clause, if provided.
     *
     * For elements, IN ranges in the direct elements of its array expression, WITHIN also ranges in its descendants.
     */
    public static WhenBuilder arrayWithin(Expression arrayExpression, String variable, Expression expression) {
        return new WhenBuilder(x("ARRAY " + arrayExpression.toString() + " FOR"),
                variable, expression, false);
    }

    /**
     * Create a FIRST comprehension with a first IN range.
     *
     * The FIRST operator lets you map and filter the elements or attributes of a collection, object, or objects.
     * It evaluates to a single element based on the operand expression that satisfies the WHEN clause, if provided.
     *
     * For each variable, IN ranges in the direct elements of its array expression, WITHIN also ranges in its descendants.
     */
    public static WhenBuilder firstIn(Expression arrayExpression, String variable, Expression expression) {
        return new WhenBuilder(x("FIRST " + arrayExpression.toString() + " FOR"),
                variable, expression, true);
    }

    /**
     * Create a FIRST comprehension with a first WITHIN range.
     *
     * The FIRST operator lets you map and filter the elements or attributes of a collection, object, or objects.
     * It evaluates to a single element based on the operand expression that satisfies the WHEN clause, if provided.
     *
     * For each variable, IN ranges in the direct elements of its array expression, WITHIN also ranges in its descendants.
     */
    public static WhenBuilder firstWithin(Expression arrayExpression, String variable, Expression expression) {
        return new WhenBuilder(x("FIRST " + arrayExpression.toString() + " FOR"),
                variable, expression, false);
    }

}
