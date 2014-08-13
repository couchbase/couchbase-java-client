/**
 * Copyright (C) 2014 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */
package com.couchbase.client.java;

/**
 * Defines the possible replication constraints to observe.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public enum ReplicateTo {

    /**
     * Do not observe any replication constraint.
     */
    NONE((short) 0),

    /**
     * Observe replication to one replica.
     */
    ONE((short) 1),

    /**
     * Observe replication to two replicas.
     */
    TWO((short) 2),

    /**
     * Observe replication to three replicas.
     */
    THREE((short) 3);

    /**
     * Contains the internal value to map onto.
     */
    private final short value;

    /**
     * Internal constructor for the enum.
     *
     * @param value the value of the replication constraint.
     */
    ReplicateTo(short value) {
        this.value = value;
    }

    /**
     * Returns the actual internal replication representation for the enum.
     *
     * @return the internal replication representation.
     */
    public short value() {
        return value;
    }

    /**
     * Identifies if this enum property will touch a replica or just the master.
     *
     * @return true if it includes a replica, false if not.
     */
    public boolean touchesReplica() {
        return value > 0;
    }
}
