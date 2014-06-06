package com.couchbase.client.java.query.dsl.element;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class LimitElement implements Element {

    private final int limit;

    public LimitElement(int limit) {
        this.limit = limit;
    }

    @Override
    public String export() {
        return "LIMIT " + limit;
    }
}
