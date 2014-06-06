package com.couchbase.client.java.query.dsl.element;

import com.couchbase.client.java.query.dsl.Expression;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class HavingElement implements Element {

    private final Expression expression;

    public HavingElement(Expression expression) {
        this.expression = expression;
    }

    @Override
    public String export() {
        return "HAVING " + expression.toString();
    }

}
