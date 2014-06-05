package com.couchbase.client.java.query;

import com.couchbase.client.core.lang.Tuple;
import com.couchbase.client.core.lang.Tuple2;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a versatile N1QL Expression.
 *
 * @author Michael Nitschinger
 * @since 1.0
 */
public class Expression {

    private final String identifier;
    private final List<Tuple2<String, Expression>> links;

    private Expression(String identifier) {
        links = new ArrayList<Tuple2<String, Expression>>();
        this.identifier = identifier;
    }

    public static Expression x(String identifier) {
        return new Expression(identifier);
    }

    public Expression eq(Expression expression) {
        links.add(Tuple.create("=", expression));
        return this;
    }

    public Expression ne(Expression expression) {
        links.add(Tuple.create("!=", expression));
        return this;
    }

    public Expression gt(Expression expression) {
        links.add(Tuple.create(">", expression));
        return this;
    }

    public Expression gte(Expression expression) {
        links.add(Tuple.create(">=", expression));
        return this;
    }

    public Expression lt(Expression expression) {
        links.add(Tuple.create("<", expression));
        return this;
    }

    public Expression lte(Expression expression) {
        links.add(Tuple.create("<=", expression));
        return this;
    }

    public Expression not(Expression expression) {
        links.add(Tuple.create("NOT", expression));
        return this;
    }

    public Expression and(Expression expression) {
        links.add(Tuple.create("AND", expression));
        return this;
    }

    public Expression or(Expression expression) {
        links.add(Tuple.create("OR", expression));
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(identifier).append(" ");
        for (Tuple2<String, Expression> link : links) {
            sb.append(link.value1()).append(" ").append(link.value2().toString());
        }
        return sb.toString();
    }
}
