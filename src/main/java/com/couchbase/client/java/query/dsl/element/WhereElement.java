package com.couchbase.client.java.query.dsl.element;

import com.couchbase.client.java.query.dsl.Expression;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class WhereElement implements Element {

    private final Expression expression;

    public WhereElement(final Expression expression) {
        this.expression = expression;
    }

    @Override
    public String export() {
        return "WHERE " + expression.toString();
    }
}
