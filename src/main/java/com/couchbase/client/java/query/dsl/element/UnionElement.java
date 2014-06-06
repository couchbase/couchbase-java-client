package com.couchbase.client.java.query.dsl.element;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class UnionElement implements Element {

    private final boolean all;

    public UnionElement(boolean all) {
        this.all = all;
    }

    @Override
    public String export() {
        StringBuilder sb = new StringBuilder();
        sb.append("UNION");
        if (all) {
            sb.append(" ALL");
        }
        return sb.toString();
    }
}
