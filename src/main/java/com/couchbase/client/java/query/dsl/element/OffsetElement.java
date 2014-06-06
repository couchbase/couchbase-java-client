package com.couchbase.client.java.query.dsl.element;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class OffsetElement implements Element {

    private int offset;

    public OffsetElement(int offset) {
        this.offset = offset;
    }

    @Override
    public String export() {
        return "OFFSET " + offset;
    }
}
