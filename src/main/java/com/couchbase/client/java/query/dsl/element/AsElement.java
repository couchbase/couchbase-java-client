package com.couchbase.client.java.query.dsl.element;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class AsElement implements Element {

    private final String as;

    public AsElement(String as) {
        this.as = as;
    }

    @Override
    public String export() {
        return "AS " + as;
    }
}
