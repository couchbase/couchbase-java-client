package com.couchbase.client.java.query.dsl.element;

import com.couchbase.client.java.query.dsl.path.JoinType;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class UnnestElement implements Element {

    private final JoinType joinType;
    private final String path;

    public UnnestElement(JoinType joinType, String path) {
        this.joinType = joinType;
        this.path = path;
    }

    @Override
    public String export() {
        StringBuilder sb = new StringBuilder();
        if (joinType != JoinType.DEFAULT) {
            sb.append(joinType.value()).append(" ");
        }
        sb.append("UNNEST ").append(path);
        return sb.toString();
    }
}
