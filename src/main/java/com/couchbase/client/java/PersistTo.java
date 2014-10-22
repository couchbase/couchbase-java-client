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

import com.couchbase.client.core.message.observe.Observe;

/**
 * Defines the possible disk persistence constraints to observe.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public enum PersistTo {

    /**
     * Observe disk persistence to the master node of the document only.
     */
    MASTER(Observe.PersistTo.MASTER),

    /**
     * Do not observe any disk persistence constraint.
     */
    NONE(Observe.PersistTo.NONE),

    /**
     * Observe disk persistence of one node (master or replica).
     */
    ONE(Observe.PersistTo.ONE),

    /**
     * Observe disk persistence of two nodes (master or replica).
     */
    TWO(Observe.PersistTo.TWO),

    /**
     * Observe disk persistence of three nodes (master or replica).
     */
    THREE(Observe.PersistTo.THREE),

    /**
     * Observe disk persistence of four nodes (one master and three replicas).
     */
    FOUR(Observe.PersistTo.FOUR);

    /**
     * Contains the internal value to map onto.
     */
    private final Observe.PersistTo value;

    /**
     * Internal constructor for the enum.
     *
     * @param value the value of the persistence constraint.
     */
    PersistTo(Observe.PersistTo value) {
        this.value = value;
    }

    /**
     * Returns the actual internal persistence representation for the enum.
     *
     * @return the internal persistence representation.
     */
    public Observe.PersistTo value() {
        return value;
    }

    /**
     * Identifies if this enum property will touch a replica or just the master.
     *
     * @return true if it includes a replica, false if not.
     */
    public boolean touchesReplica() {
        return value.touchesReplica();
    }
}
