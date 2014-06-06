package com.couchbase.client.java.query.dsl;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;

/**
 * Represents a N1QL Expression.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class Expression {

    private static final Expression NULL_INSTANCE = new Expression("NULL");
    private static final Expression TRUE_INSTANCE = new Expression(true);
    private static final Expression FALSE_INSTANCE = new Expression(false);

    private final Object value;

    private Expression(final Object value) {
        this.value = value;
    }

    public static Expression x(String value) {
        return new Expression(value);
    }

    public static Expression s(String value) {
        return new Expression("\"" + value + "\"");
    }

    public static Expression x(int value) {
        return new Expression(value);
    }

    public static Expression x(long value) {
        return new Expression(value);
    }

    public static Expression x(boolean value) {
        return value ? TRUE_INSTANCE : FALSE_INSTANCE;
    }

    public static Expression x(JsonArray value) {
        return new Expression(value);
    }

    public static Expression x(JsonObject value) {
        return new Expression(value);
    }

    public static Expression TRUE() {
        return TRUE_INSTANCE;
    }

    public static Expression FALSE() {
        return FALSE_INSTANCE;
    }

    public static Expression NULL() {
        return NULL_INSTANCE;
    }

    public Expression not() {
        return prefix("NOT", toString());
    }

    public Expression and(Expression right) {
        return infix("AND", toString(), right.toString());
    }

    public Expression or(Expression right) {
        return infix("OR", toString(), right.toString());
    }

    public Expression eq(Expression right) {
        return infix("=", toString(), right.toString());
    }

    public Expression ne(Expression right) {
        return infix("!=", toString(), right.toString());
    }

    public Expression gt(Expression right) {
        return infix(">", toString(), right.toString());
    }

    public Expression lt(Expression right) {
        return infix("<", toString(), right.toString());
    }

    public Expression gte(Expression right) {
        return infix(">=", toString(), right.toString());
    }

    public Expression lte(Expression right) {
        return infix("<=", toString(), right.toString());
    }

    public Expression is(Expression right) {
        return infix("IS", toString(), right.toString());
    }

    public Expression between(Expression right) {
        return infix("BETWEEN", toString(), right.toString());
    }

    public Expression notBetween(Expression right) {
        return infix("NOT BETWEEN", toString(), right.toString());
    }

    public Expression like(Expression right) {
        return infix("LIKE", toString(), right.toString());
    }

    public Expression notLike(Expression right) {
        return infix("NOT LIKE", toString(), right.toString());
    }

    public Expression isValued() {
        return postfix("IS VALUED", toString());
    }

    public Expression isNotValued() {
        return postfix("IS NOT VALUED", toString());
    }

    public Expression isNull() {
        return postfix("IS NULL", toString());
    }

    public Expression isNotNull() {
        return postfix("IS NOT NULL", toString());
    }

    public Expression isMissing() {
        return postfix("IS MISSING", toString());
    }

    public Expression isNotMissing() {
        return postfix("IS NOT MISSING", toString());
    }

    public Expression concat(Expression right) {
        return infix("||", toString(), right.toString());
    }

    public Expression isNull(Expression right) {
        return infix("<=", toString(), right.toString());
    }

    public Expression exists() {
        return prefix("EXISTS", toString());
    }

    public Expression in(Expression right) {
        return infix("IN", toString(), right.toString());
    }

    public Expression notIn(Expression right) {
        return infix("NOT IN", toString(), right.toString());
    }

    public Expression as(String alias) {
        return infix("AS", toString(), alias);
    }

    private static Expression prefix(String prefix, String right) {
        return new Expression(prefix + " " + right);
    }

    private static Expression infix(String infix, String left, String right) {
        return new Expression(left + " " + infix + " " + right);
    }

    private static Expression postfix(String postfix, String left) {
        return new Expression(left + " " + postfix);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
