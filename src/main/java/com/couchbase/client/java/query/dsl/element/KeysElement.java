package com.couchbase.client.java.query.dsl.element;

import com.couchbase.client.java.query.dsl.Expression;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class KeysElement implements Element {

    private final Expression expression;

    public KeysElement(Expression expression) {
        this.expression = expression;
    }

    @Override
    public String export() {
        return "KEYS " + expression.toString();
    }
}
