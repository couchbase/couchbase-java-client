package com.couchbase.client.java.query.dsl.element;

import com.couchbase.client.java.query.dsl.path.JoinType;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class JoinElement implements Element {

    private final JoinType joinType;
    private final String from;

    public JoinElement(JoinType joinType, String from) {
        this.joinType = joinType;
        this.from = from;
    }

    @Override
    public String export() {
        StringBuilder sb = new StringBuilder();
        if (joinType != JoinType.DEFAULT) {
            sb.append(joinType.value()).append(" ");
        }
        sb.append("JOIN ").append(from);
        return sb.toString();
    }
}
