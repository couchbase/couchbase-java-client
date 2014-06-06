package com.couchbase.client.java.query.dsl.element;

import com.couchbase.client.java.query.dsl.Expression;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class GroupByElement implements Element {

    private final Expression[] expressions;

    public GroupByElement(final Expression[] expressions) {
        this.expressions = expressions;
    }

    @Override
    public String export() {
        StringBuilder sb = new StringBuilder();
        sb.append("GROUP BY ");
        for (int i = 0; i < expressions.length; i++) {
            sb.append(expressions[i].toString());
            if (i < expressions.length-1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
