package com.couchbase.client.java.query.dsl.element;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class FromElement implements Element {

    private final String from;

    public FromElement(String from) {
        this.from = from;
    }

    @Override
    public String export() {
        return "FROM " + from;
    }
}
