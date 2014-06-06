package com.couchbase.client.java.query.dsl.element;

import com.couchbase.client.java.query.dsl.Sort;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class OrderByElement implements Element {

    private final Sort[] sorts;

    public OrderByElement(Sort... sorts) {
        this.sorts = sorts;
    }

    @Override
    public String export() {
        StringBuilder sb = new StringBuilder();
        sb.append("ORDER BY ");
        for (int i = 0; i < sorts.length; i++) {
            sb.append(sorts[i].toString());
            if (i < sorts.length-1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
