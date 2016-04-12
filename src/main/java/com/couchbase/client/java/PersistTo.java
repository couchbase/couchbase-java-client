/*
 * Copyright (c) 2016 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
