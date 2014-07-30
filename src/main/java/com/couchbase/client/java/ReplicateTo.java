package com.couchbase.client.java;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public enum ReplicateTo {

    NONE((short) 0),

    ONE((short) 1),

    TWO((short) 2),

    THREE((short) 3);

    private final short value;

    ReplicateTo(short value) {
        this.value = value;
    }

    public short value() {
        return value;
    }
}
