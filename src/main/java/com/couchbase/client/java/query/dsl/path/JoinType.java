package com.couchbase.client.java.query.dsl.path;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public enum JoinType {

    DEFAULT(""),
    INNER("INNER"),
    LEFT("LEFT"),
    LEFT_OUTER("LEFT OUTER");

    private final String value;

    JoinType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
