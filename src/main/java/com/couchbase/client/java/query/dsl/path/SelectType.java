package com.couchbase.client.java.query.dsl.path;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public enum SelectType {

    DEFAULT(""),
    ALL("ALL"),
    DISTINCT("DISTINCT"),
    RAW("RAW");

    private final String value;

    SelectType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
