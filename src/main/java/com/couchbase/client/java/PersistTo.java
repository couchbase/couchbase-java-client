package com.couchbase.client.java;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public enum PersistTo {

    MASTER((short) -1),

    NONE((short) 0),

    ONE((short) 1),

    TWO((short) 2),

    THREE((short) 3),

    FOUR((short) 4);

    private final short value;

    PersistTo(short value) {
        this.value = value;
    }

    public short value() {
        return value;
    }
}
