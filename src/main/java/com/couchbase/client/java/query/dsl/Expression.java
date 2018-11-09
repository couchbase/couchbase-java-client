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

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.Statement;

/**
 * Represents a N1QL Expression.
 *
 * @author Michael Nitschinger
 * @since 2.0.0
 */
public class Expression {

    private static final Expression NULL_INSTANCE = new Expression("NULL");
    private static final Expression TRUE_INSTANCE = new Expression("TRUE");
    private static final Expression FALSE_INSTANCE = new Expression("FALSE");
    private static final Expression MISSING_INSTANCE = new Expression("MISSING");
    private static final Expression EMPTY_INSTANCE = new Expression("");

    private final Object value;

    private Expression(final Object value) {
        this.value = value;
    }

    /**
     * Creates an arbitrary expression from the given string value.
     *
     * No quoting or escaping will be done on the input. In addition, it is not checked if the given value
     * is an actual valid (N1QL syntax wise) expression.
     *
     * @param value the value to create the expression from.
     * @return a new {@link Expression} representing the value.
     */
    public static Expression x(final String value) {
        return new Expression(value);
    }

    /**
     * Creates an arbitrary expression from the given integer value.
     *
     * No quoting or escaping will be done on the input. In addition, it is not checked if the given value
     * is an actual valid (N1QL syntax wise) expression.
     *
     * @param value the value to create the expression from.
     * @return a new {@link Expression} representing the value.
     */
    public static Expression x(final int value) {
        return new Expression(value);
    }

    /**
     * Creates an arbitrary expression from the given long value.
     *
     * No quoting or escaping will be done on the input. In addition, it is not checked if the given value
     * is an actual valid (N1QL syntax wise) expression.
     *
     * @param value the value to create the expression from.
     * @return a new {@link Expression} representing the value.
     */
    public static Expression x(final long value) {
        return new Expression(value);
    }

    /**
     * Creates an arbitrary expression from the given boolean value.
     *
     * No quoting or escaping will be done on the input. In addition, it is not checked if the given value
     * is an actual valid (N1QL syntax wise) expression.
     *
     * @param value the value to create the expression from.
     * @return a new {@link Expression} representing the value.
     */
    public static Expression x(final boolean value) {
        return value ? TRUE_INSTANCE : FALSE_INSTANCE;
    }

    /**
     * Creates an arbitrary expression from the given double value.
     *
     * No quoting or escaping will be done on the input. In addition, it is not checked if the given value
     * is an actual valid (N1QL syntax wise) expression.
     *
     * @param value the value to create the expression from.
     * @return a new {@link Expression} representing the value.
     */
    public static Expression x(final double value) {
        return new Expression(value);
    }

    /**
     * Creates an arbitrary expression from the given float value.
     *
     * No quoting or escaping will be done on the input. In addition, it is not checked if the given value
     * is an actual valid (N1QL syntax wise) expression.
     *
     * @param value the value to create the expression from.
     * @return a new {@link Expression} representing the value.
     */
    public static Expression x(final float value) {
        return new Expression(value);
    }

    /**
     * Creates an arbitrary expression from the given json array.
     *
     * No quoting or escaping will be done on the input. In addition, it is not checked if the given value
     * is an actual valid (N1QL syntax wise) expression.
     *
     * @param value the value to create the expression from.
     * @return a new {@link Expression} representing the value.
     */
    public static Expression x(final JsonArray value) {
        return new Expression(value);
    }

    /**
     * Creates an arbitrary expression from the given json object.
     *
     * No quoting or escaping will be done on the input. In addition, it is not checked if the given value
     * is an actual valid (N1QL syntax wise) expression.
     *
     * @param value the value to create the expression from.
     * @return a new {@link Expression} representing the value.
     */
    public static Expression x(final JsonObject value) {
        return new Expression(value);
    }

    /**
     * Creates an expression for a given {@link Statement}, as is.
     *
     * @param statement the statement to convert to an expression.
     * @return the statement, converted as is into an expression.
     */
    public static Expression x(final Statement statement) {
        return x(statement.toString());
    }

    /**
     * Creates an expression from a {@link Number}, as is.
     *
     * @param number the number constant to convert to an expression.
     * @return the number converted into an expression.
     */
    public static Expression x(final Number number) {
        return x(String.valueOf(number));
    }

    /**
     * Creates an expression from a given sub-{@link Statement}, wrapping it in parenthesis.
     *
     * @param statement the statement to convert to an expression.
     * @return the statement, converted into an expression wrapped in parenthesis.
     */
    public static Expression sub(final Statement statement) {
        return x("(" + statement.toString() + ")");
    }

    /**
     * Puts an {@link Expression} in parenthesis.
     *
     * @param expression the expression to wrap in parenthesis.
     * @return the expression, wrapped in parenthesis.
     */
    public static Expression par(final Expression expression) {
        return infix(expression.toString(), "(", ")");
    }

    /**
     * Construct a path ("a.b.c") from Expressions or values. Strings are considered identifiers
     * (so they won't be quoted).
     *
     * @param pathComponents the elements of the path, joined together by a dot.
     * @return the path created from the given components.
     */
    public static Expression path(Object... pathComponents) {
        if (pathComponents == null || pathComponents.length == 0) {
            return EMPTY_INSTANCE;
        }
        StringBuilder path = new StringBuilder();
        for (Object p : pathComponents) {
            path.append('.');
            if (p instanceof Expression) {
                path.append(((Expression) p).toString());
            } else {
                path.append(String.valueOf(p));
            }
        }
        path.deleteCharAt(0);
        return x(path.toString());
    }

    /**
     * An identifier or list of identifiers escaped using backquotes `.
     *
     * Useful for example for identifiers that contains a dash like "beer-sample".
     * Multiple identifiers are returned as a list of escaped identifiers separated by ", ".
     *
     * @param identifiers the identifier(s) to escape.
     * @return an {@link Expression} representing the escaped identifier.
     */
    public static Expression i(final String... identifiers) {
        return new Expression(wrapWith('`', identifiers));
    }

    /**
     * An identifier or list of identifiers which will be quoted as strings (with "").
     *
     * @param strings the list of strings to quote.
     * @return an {@link Expression} representing the quoted strings.
     */
    public static Expression s(final String... strings) {
        return new Expression(wrapWith('"', strings));
    }

    /**
     * Returns an expression representing boolean TRUE.
     *
     * @return an expression representing TRUE.
     */
    public static Expression TRUE() {
        return TRUE_INSTANCE;
    }

    /**
     * Returns an expression representing boolean FALSE.
     *
     * @return an expression representing FALSE.
     */
    public static Expression FALSE() {
        return FALSE_INSTANCE;
    }

    /**
     * Returns an expression representing NULL.
     *
     * @return an expression representing NULL.
     */
    public static Expression NULL() {
        return NULL_INSTANCE;
    }

    /**
     * Returns an expression representing MISSING.
     *
     * @return an expression representing MISSING.
     */
    public static Expression MISSING() {
        return MISSING_INSTANCE;
    }

    /**
     * Negates the given expression by prefixing a NOT.
     *
     * @return the negated expression.
     */
    public Expression not() {
        return prefix("NOT", toString());
    }

    /**
     * AND-combines two expressions.
     *
     * @param right the expression to combine with the current one.
     * @return a combined expression.
     */
    public Expression and(Expression right) {
        return infix("AND", toString(), right.toString());
    }

    /**
     * AND-combines two expressions.
     *
     * @param right the expression to combine with the current one.
     * @return a combined expression.
     */
    public Expression and(String right) {
        return and(x(right));
    }

    /**
     * AND-combines two expressions.
     *
     * @param right the expression to combine with the current one.
     * @return a combined expression.
     */
    public Expression and(int right) {
        return and(x(right));
    }

    /**
     * AND-combines two expressions.
     *
     * @param right the expression to combine with the current one.
     * @return a combined expression.
     */
    public Expression and(long right) {
        return and(x(right));
    }

    /**
     * AND-combines two expressions.
     *
     * @param right the expression to combine with the current one.
     * @return a combined expression.
     */
    public Expression and(float right) {
        return and(x(right));
    }

    /**
     * AND-combines two expressions.
     *
     * @param right the expression to combine with the current one.
     * @return a combined expression.
     */
    public Expression and(double right) {
        return and(x(right));
    }

    /**
     * AND-combines two expressions.
     *
     * @param right the expression to combine with the current one.
     * @return a combined expression.
     */
    public Expression and(boolean right) {
        return and(x(right));
    }

    /**
     * AND-combines two expressions.
     *
     * @param right the expression to combine with the current one.
     * @return a combined expression.
     */
    public Expression and(JsonObject right) {
        return and(x(right));
    }

    /**
     * AND-combines two expressions.
     *
     * @param right the expression to combine with the current one.
     * @return a combined expression.
     */
    public Expression and(JsonArray right) {
        return and(x(right));
    }

    /**
     * OR-combines two expressions.
     *
     * @param right the expression to combine with the current one.
     * @return a combined expression.
     */
    public Expression or(Expression right) {
        return infix("OR", toString(), right.toString());
    }

    /**
     * OR-combines two expressions.
     *
     * @param right the expression to combine with the current one.
     * @return a combined expression.
     */
    public Expression or(String right) {
        return or(x(right));
    }

    /**
     * OR-combines two expressions.
     *
     * @param right the expression to combine with the current one.
     * @return a combined expression.
     */
    public Expression or(int right) {
        return or(x(right));
    }

    /**
     * OR-combines two expressions.
     *
     * @param right the expression to combine with the current one.
     * @return a combined expression.
     */
    public Expression or(long right) {
        return or(x(right));
    }

    /**
     * OR-combines two expressions.
     *
     * @param right the expression to combine with the current one.
     * @return a combined expression.
     */
    public Expression or(boolean right) {
        return or(x(right));
    }

    /**
     * OR-combines two expressions.
     *
     * @param right the expression to combine with the current one.
     * @return a combined expression.
     */
    public Expression or(float right) {
        return or(x(right));
    }

    /**
     * OR-combines two expressions.
     *
     * @param right the expression to combine with the current one.
     * @return a combined expression.
     */
    public Expression or(double right) {
        return or(x(right));
    }

    /**
     * OR-combines two expressions.
     *
     * @param right the expression to combine with the current one.
     * @return a combined expression.
     */
    public Expression or(JsonArray right) {
        return or(x(right));
    }

    /**
     * OR-combines two expressions.
     *
     * @param right the expression to combine with the current one.
     * @return a combined expression.
     */
    public Expression or(JsonObject right) {
        return or(x(right));
    }

    /**
     * Combines two expressions with the equals operator ("=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression eq(Expression right) {
        return infix("=", toString(), right.toString());
    }

    /**
     * Combines two expressions with the equals operator ("=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression eq(String right) {
        return eq(x(right));
    }

    /**
     * Combines two expressions with the equals operator ("=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression eq(int right) {
        return eq(x(right));
    }

    /**
     * Combines two expressions with the equals operator ("=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression eq(long right) {
        return eq(x(right));
    }

    /**
     * Combines two expressions with the equals operator ("=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression eq(float right) {
        return eq(x(right));
    }

    /**
     * Combines two expressions with the equals operator ("=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression eq(double right) {
        return eq(x(right));
    }

    /**
     * Combines two expressions with the equals operator ("=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression eq(boolean right) {
        return eq(x(right));
    }

    /**
     * Combines two expressions with the equals operator ("=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression eq(JsonArray right) {
        return eq(x(right));
    }

    /**
     * Combines two expressions with the equals operator ("=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression eq(JsonObject right) {
        return eq(x(right));
    }

    /**
     * Combines two expressions with the not equals operator ("!=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression ne(Expression right) {
        return infix("!=", toString(), right.toString());
    }

    /**
     * Combines two expressions with the not equals operator ("!=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression ne(String right) {
        return ne(x(right));
    }

    /**
     * Combines two expressions with the not equals operator ("!=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression ne(int right) {
        return ne(x(right));
    }

    /**
     * Combines two expressions with the not equals operator ("!=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression ne(long right) {
        return ne(x(right));
    }

    /**
     * Combines two expressions with the not equals operator ("!=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression ne(double right) {
        return ne(x(right));
    }

    /**
     * Combines two expressions with the not equals operator ("!=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression ne(float right) {
        return ne(x(right));
    }

    /**
     * Combines two expressions with the not equals operator ("!=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression ne(JsonObject right) {
        return ne(x(right));
    }

    /**
     * Combines two expressions with the not equals operator ("!=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression ne(JsonArray right) {
        return ne(x(right));
    }

    /**
     * Combines two expressions with the greater than operator ("&gt;").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression gt(Expression right) {
        return infix(">", toString(), right.toString());
    }

    /**
     * Combines two expressions with the greater than operator ("&gt;").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression gt(String right) {
        return gt(x(right));
    }

    /**
     * Combines two expressions with the greater than operator ("&gt;").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression gt(int right) {
        return gt(x(right));
    }

    /**
     * Combines two expressions with the greater than operator ("&gt;").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression gt(long right) {
        return gt(x(right));
    }

    /**
     * Combines two expressions with the greater than operator ("&gt;").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression gt(float right) {
        return gt(x(right));
    }

    /**
     * Combines two expressions with the greater than operator ("&gt;").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression gt(double right) {
        return gt(x(right));
    }

    /**
     * Combines two expressions with the greater than operator ("&gt;").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression gt(boolean right) {
        return gt(x(right));
    }

    /**
     * Combines two expressions with the greater than operator ("&gt;").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression gt(JsonArray right) {
        return gt(x(right));
    }

    /**
     * Combines two expressions with the greater than operator ("&gt;").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression gt(JsonObject right) {
        return gt(x(right));
    }

    /**
     * Combines two expressions with the less than operator ("&lt;").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression lt(Expression right) {
        return infix("<", toString(), right.toString());
    }

    /**
     * Combines two expressions with the less than operator ("&lt;").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression lt(String right) {
        return lt(x(right));
    }

    /**
     * Combines two expressions with the less than operator ("&lt;").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression lt(int right) {
        return lt(x(right));
    }

    /**
     * Combines two expressions with the less than operator ("&lt;").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression lt(long right) {
        return lt(x(right));
    }

    /**
     * Combines two expressions with the less than operator ("&lt;").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression lt(double right) {
        return lt(x(right));
    }

    /**
     * Combines two expressions with the less than operator ("&lt;").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression lt(float right) {
        return lt(x(right));
    }

    /**
     * Combines two expressions with the less than operator ("&lt;").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression lt(boolean right) {
        return lt(x(right));
    }

    /**
     * Combines two expressions with the less than operator ("&lt;").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression lt(JsonObject right) {
        return lt(x(right));
    }

    /**
     * Combines two expressions with the less than operator ("&lt;").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression lt(JsonArray right) {
        return lt(x(right));
    }

    /**
     * Combines two expressions with the greater or equals than operator ("&gt;=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression gte(Expression right) {
        return infix(">=", toString(), right.toString());
    }

    /**
     * Combines two expressions with the greater or equals than operator ("&gt;=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression gte(String right) {
        return gte(x(right));
    }

    /**
     * Combines two expressions with the greater or equals than operator ("&gt;=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression gte(int right) {
        return gte(x(right));
    }

    /**
     * Combines two expressions with the greater or equals than operator ("&gt;=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression gte(long right) {
        return gte(x(right));
    }

    /**
     * Combines two expressions with the greater or equals than operator ("&gt;=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression gte(double right) {
        return gte(x(right));
    }

    /**
     * Combines two expressions with the greater or equals than operator ("&gt;=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression gte(float right) {
        return gte(x(right));
    }

    /**
     * Combines two expressions with the greater or equals than operator ("&gt;=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression gte(boolean right) {
        return gte(x(right));
    }

    /**
     * Combines two expressions with the greater or equals than operator ("&gt;=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression gte(JsonObject right) {
        return gte(x(right));
    }

    /**
     * Combines two expressions with the greater or equals than operator ("&gt;=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression gte(JsonArray right) {
        return gte(x(right));
    }

    /**
     * Combines two expressions with the concatenation operator ("||").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression concat(Expression right) {
        return infix("||", toString(), right.toString());
    }

    /**
     * Combines two expressions with the concatenation operator ("||").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression concat(String right) {
        return concat(x(right));
    }

    /**
     * Combines two expressions with the concatenation operator ("||").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression concat(int right) {
        return concat(x(right));
    }

    /**
     * Combines two expressions with the concatenation operator ("||").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression concat(long right) {
        return concat(x(right));
    }

    /**
     * Combines two expressions with the concatenation operator ("||").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression concat(boolean right) {
        return concat(x(right));
    }

    /**
     * Combines two expressions with the concatenation operator ("||").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression concat(float right) {
        return concat(x(right));
    }

    /**
     * Combines two expressions with the concatenation operator ("||").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression concat(double right) {
        return concat(x(right));
    }

    /**
     * Combines two expressions with the concatenation operator ("||").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression concat(JsonObject right) {
        return concat(x(right));
    }

    /**
     * Combines two expressions with the concatenation operator ("||").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression concat(JsonArray right) {
        return concat(x(right));
    }

    /**
     * Combines two expressions with the less or equals than operator ("&lt;=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression lte(Expression right) {
        return infix("<=", toString(), right.toString());
    }

    /**
     * Combines two expressions with the less or equals than operator ("&lt;=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression lte(String right) {
        return lte(x(right));
    }

    /**
     * Combines two expressions with the less or equals than operator ("&lt;=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression lte(int right) {
        return lte(x(right));
    }

    /**
     * Combines two expressions with the less or equals than operator ("&lt;=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression lte(long right) {
        return lte(x(right));
    }

    /**
     * Combines two expressions with the less or equals than operator ("&lt;=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression lte(float right) {
        return lte(x(right));
    }

    /**
     * Combines two expressions with the less or equals than operator ("&lt;=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression lte(double right) {
        return lte(x(right));
    }

    /**
     * Combines two expressions with the less or equals than operator ("&lt;=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression lte(boolean right) {
        return lte(x(right));
    }

    /**
     * Combines two expressions with the less or equals than operator ("&lt;=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression lte(JsonObject right) {
        return lte(x(right));
    }

    /**
     * Combines two expressions with the less or equals than operator ("&lt;=").
     *
     * @param right the expression to combine.
     * @return the combined expressions.
     */
    public Expression lte(JsonArray right) {
        return lte(x(right));
    }

    /**
     * Appends a "IS VALUED" to the expression.
     *
     * @return the postfixed expression.
     */
    public Expression isValued() {
        return postfix("IS VALUED", toString());
    }

    /**
     * Appends a "IS NOT VALUED" to the expression.
     *
     * @return the postfixed expression.
     */
    public Expression isNotValued() {
        return postfix("IS NOT VALUED", toString());
    }

    /**
     * Appends a "IS NULL" to the expression.
     *
     * @return the postfixed expression.
     */
    public Expression isNull() {
        return postfix("IS NULL", toString());
    }

    /**
     * Appends a "IS NOT NULL" to the expression.
     *
     * @return the postfixed expression.
     */
    public Expression isNotNull() {
        return postfix("IS NOT NULL", toString());
    }

    /**
     * Appends a "IS MISSING" to the expression.
     *
     * @return the postfixed expression.
     */
    public Expression isMissing() {
        return postfix("IS MISSING", toString());
    }

    /**
     * Appends a "IS NOT MISSING" to the expression.
     *
     * @return the postfixed expression.
     */
    public Expression isNotMissing() {
        return postfix("IS NOT MISSING", toString());
    }

    /**
     * Adds a BETWEEN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression between(Expression right) {
        return infix("BETWEEN", toString(), right.toString());
    }

    /**
     * Adds a BETWEEN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression between(String right) {
        return between(x(right));
    }

    /**
     * Adds a BETWEEN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression between(int right) {
        return between(x(right));
    }

    /**
     * Adds a BETWEEN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression between(long right) {
        return between(x(right));
    }

    /**
     * Adds a BETWEEN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression between(double right) {
        return between(x(right));
    }

    /**
     * Adds a BETWEEN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression between(float right) {
        return between(x(right));
    }

    /**
     * Adds a BETWEEN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression between(boolean right) {
        return between(x(right));
    }

    /**
     * Adds a BETWEEN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression between(JsonObject right) {
        return between(x(right));
    }

    /**
     * Adds a BETWEEN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression between(JsonArray right) {
        return between(x(right));
    }

    /**
     * Adds a NOT BETWEEN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notBetween(Expression right) {
        return infix("NOT BETWEEN", toString(), right.toString());
    }

    /**
     * Adds a NOT BETWEEN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notBetween(String right) {
        return notBetween(x(right));
    }

    /**
     * Adds a NOT BETWEEN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notBetween(int right) {
        return notBetween(x(right));
    }

    /**
     * Adds a NOT BETWEEN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notBetween(long right) {
        return notBetween(x(right));
    }

    /**
     * Adds a NOT BETWEEN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notBetween(double right) {
        return notBetween(x(right));
    }

    /**
     * Adds a NOT BETWEEN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notBetween(float right) {
        return notBetween(x(right));
    }

    /**
     * Adds a NOT BETWEEN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notBetween(boolean right) {
        return notBetween(x(right));
    }

    /**
     * Adds a NOT BETWEEN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notBetween(JsonObject right) {
        return notBetween(x(right));
    }

    /**
     * Adds a NOT BETWEEN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notBetween(JsonArray right) {
        return notBetween(x(right));
    }

    /**
     * Adds a LIKE clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression like(Expression right) {
        return infix("LIKE", toString(), right.toString());
    }

    /**
     * Adds a LIKE clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression like(String right) {
        return like(x(right));
    }

    /**
     * Adds a LIKE clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression like(int right) {
        return like(x(right));
    }

    /**
     * Adds a LIKE clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression like(long right) {
        return like(x(right));
    }

    /**
     * Adds a LIKE clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression like(boolean right) {
        return like(x(right));
    }

    /**
     * Adds a LIKE clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression like(double right) {
        return like(x(right));
    }

    /**
     * Adds a LIKE clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression like(float right) {
        return like(x(right));
    }

    /**
     * Adds a LIKE clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression like(JsonObject right) {
        return like(x(right));
    }

    /**
     * Adds a LIKE clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression like(JsonArray right) {
        return like(x(right));
    }

    /**
     * Adds a NOT LIKE clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notLike(Expression right) {
        return infix("NOT LIKE", toString(), right.toString());
    }

    /**
     * Adds a NOT LIKE clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notLike(String right) {
        return notLike(x(right));
    }

    /**
     * Adds a NOT LIKE clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notLike(int right) {
        return notLike(x(right));
    }

    /**
     * Adds a NOT LIKE clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notLike(long right) {
        return notLike(x(right));
    }

    /**
     * Adds a NOT LIKE clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notLike(boolean right) {
        return notLike(x(right));
    }

    /**
     * Adds a NOT LIKE clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notLike(float right) {
        return notLike(x(right));
    }

    /**
     * Adds a NOT LIKE clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notLike(double right) {
        return notLike(x(right));
    }

    /**
     * Adds a NOT LIKE clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notLike(JsonObject right) {
        return notLike(x(right));
    }

    /**
     * Adds a NOT LIKE clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notLike(JsonArray right) {
        return notLike(x(right));
    }

    /**
     * Prefixes the current expression with the EXISTS clause.
     *
     * @return a new expression with the clause applied.
     */
    public Expression exists() {
        return prefix("EXISTS", toString());
    }

    /**
     * Adds a IN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression in(Expression right) {
        return infix("IN", toString(), right.toString());
    }

    /**
     * Adds a IN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression in(String right) {
        return in(x(right));
    }

    /**
     * Adds a IN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression in(int right) {
        return in(x(right));
    }

    /**
     * Adds a IN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression in(long right) {
        return in(x(right));
    }

    /**
     * Adds a IN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression in(boolean right) {
        return in(x(right));
    }

    /**
     * Adds a IN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression in(double right) {
        return in(x(right));
    }

    /**
     * Adds a IN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression in(float right) {
        return in(x(right));
    }

    /**
     * Adds a IN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression in(JsonObject right) {
        return in(x(right));
    }

    /**
     * Adds a IN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression in(JsonArray right) {
        return in(x(right));
    }

    /**
     * Adds a NOT IN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notIn(Expression right) {
        return infix("NOT IN", toString(), right.toString());
    }

    /**
     * Adds a NOT IN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notIn(String right) {
        return notIn(x(right));
    }

    /**
     * Adds a NOT IN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notIn(int right) {
        return notIn(x(right));
    }

    /**
     * Adds a NOT IN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notIn(long right) {
        return notIn(x(right));
    }

    /**
     * Adds a NOT IN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notIn(boolean right) {
        return notIn(x(right));
    }

    /**
     * Adds a NOT IN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notIn(float right) {
        return notIn(x(right));
    }

    /**
     * Adds a NOT IN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notIn(double right) {
        return notIn(x(right));
    }

    /**
     * Adds a NOT IN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notIn(JsonObject right) {
        return notIn(x(right));
    }

    /**
     * Adds a NOT IN clause between the current and the given expression.
     *
     * @param right the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression notIn(JsonArray right) {
        return notIn(x(right));
    }

    /**
     * Adds a AS clause between the current and the given expression. Often used to alias an identifier.
     *
     * @param alias the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression as(String alias) {
        return as(x(alias));
    }

    /**
     * Adds a AS clause between the current and the given expression. Often used to alias an identifier.
     *
     * @param alias the right hand side expression.
     * @return a new expression with the clause applied.
     */
    public Expression as(Expression alias) {
        return infix("AS", toString(), alias.toString());
    }

    //============ SIMPLE ARITHMETICS ============

    /**
     * Arithmetic addition between current and given expression.
     *
     * @param expression the right hand side expression.
     * @return the addition expression.
     */
    public Expression add(Expression expression) {
        return infix("+", toString(), expression.toString());
    }

    /**
     * Arithmetic addition between current expression and a given number.
     *
     * @param b the right hand side number.
     * @return the addition expression.
     */
    public Expression add(Number b) {
        return add(x(String.valueOf(b)));
    }

    /**
     * Arithmetic addition between current and given expression.
     *
     * @param expression the right hand side expression.
     * @return the addition expression.
     */
    public Expression add(String expression) {
        return add(x(expression));
    }

    /**
     * Arithmetic v between current and given expression.
     *
     * @param expression the right hand side expression.
     * @return the subtraction expression.
     */
    public Expression subtract(Expression expression) {
        return infix("-", toString(), expression.toString());
    }

    /**
     * Arithmetic subtraction between current expression and a given number.
     *
     * @param b the right hand side number.
     * @return the subtraction expression.
     */
    public Expression subtract(Number b) {
        return subtract(x(String.valueOf(b)));
    }

    /**
     * Arithmetic subtraction between current and given expression.
     *
     * @param expression the right hand side expression.
     * @return the subtraction expression.
     */
    public Expression subtract(String expression) {
        return subtract(x(expression));
    }

    /**
     * Arithmetic multiplication between current and given expression.
     *
     * @param expression the right hand side expression.
     * @return the multiplication expression.
     */
    public Expression multiply(Expression expression) {
        return infix("*", toString(), expression.toString());
    }

    /**
     * Arithmetic multiplication between current expression and a given number.
     *
     * @param b the right hand side number.
     * @return the multiplication expression.
     */
    public Expression multiply(Number b) {
        return multiply(x(String.valueOf(b)));
    }

    /**
     * Arithmetic multiplication between current and given expression.
     *
     * @param expression the right hand side expression.
     * @return the multiplication expression.
     */
    public Expression multiply(String expression) {
        return multiply(x(expression));
    }

    /**
     * Arithmetic division between current and given expression.
     *
     * @param expression the right hand side expression.
     * @return the division expression.
     */
    public Expression divide(Expression expression) {
        return infix("/", toString(), expression.toString());
    }

    /**
     * Arithmetic division between current expression and a given number.
     *
     * @param b the right hand side number.
     * @return the division expression.
     */
    public Expression divide(Number b) {
        return divide(x(String.valueOf(b)));
    }

    /**
     * Arithmetic division between current and given expression.
     *
     * @param expression the right hand side expression.
     * @return the division expression.
     */
    public Expression divide(String expression) {
        return divide(x(expression));
    }

    /**
     * Get attribute of an object using the given string as attribute name.
     *
     * @param expression The attribute name
     * @return the getter expression
     */
    public Expression get(String expression) {
        return new Expression(path(toString(), x(expression)));
    }

    /**
     * Get attribute of an object using the given expression as attribute name.
     *
     * @param expression The attribute name
     * @return the getter expression
     */
    public Expression get(Expression expression) {
        return get(expression.toString());
    }

    //===== HELPERS =====

    /**
     * Helper method to prefix a string.
     *
     * @param prefix the prefix.
     * @param right the right side of the expression.
     * @return a prefixed expression.
     */
    private static Expression prefix(String prefix, String right) {
        return new Expression(prefix + " " + right);
    }

    /**
     * Helper method to infix a string.
     *
     * @param infix the infix.
     * @param left the left side of the expression.
     * @param right the right side of the expression.
     * @return a infixed expression.
     */
    private static Expression infix(String infix, String left, String right) {
        return new Expression(left + " " + infix + " " + right);
    }

    /**
     * Helper method to postfix a string.
     *
     * @param postfix the postfix.
     * @param left the left side of the expression.
     * @return a prefixed expression.
     */
    private static Expression postfix(String postfix, String left) {
        return new Expression(left + " " + postfix);
    }

    /**
     * Helper method to wrap varargs with the given character.
     *
     * @param wrapper the wrapper character.
     * @param input the input fields to wrap.
     * @return a concatenated string with characters wrapped.
     */
    private static String wrapWith(char wrapper, String... input) {
        StringBuilder escaped = new StringBuilder();
        for (String i : input) {
            escaped.append(", ");
            escaped.append(wrapper).append(i).append(wrapper);
        }
        if (escaped.length() > 2) {
            escaped.delete(0, 2);
        }
        return escaped.toString();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
