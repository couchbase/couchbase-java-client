package com.couchbase.client.java.query.dsl.element;

import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.path.SelectType;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class SelectElement implements Element {

    private final SelectType selectType;
    private final Expression[] expressions;


    public SelectElement(SelectType selectType, Expression... expressions) {
        this.selectType = selectType;
        this.expressions = expressions;
    }

    @Override
    public String export() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        if (selectType != SelectType.DEFAULT) {
            sb.append(selectType).append(" ");
        }
        for (int i=0; i < expressions.length; i++) {
            sb.append(expressions[i].toString());
            if (i < expressions.length-1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
