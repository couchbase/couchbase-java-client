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
 * Defines the possible replication constraints to observe.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public enum ReplicateTo {

    /**
     * Do not observe any replication constraint.
     */
    NONE(Observe.ReplicateTo.NONE),

    /**
     * Observe replication to one replica.
     */
    ONE(Observe.ReplicateTo.ONE),

    /**
     * Observe replication to two replicas.
     */
    TWO(Observe.ReplicateTo.TWO),

    /**
     * Observe replication to three replicas.
     */
    THREE(Observe.ReplicateTo.THREE);

    /**
     * Contains the internal value to map onto.
     */
    private final Observe.ReplicateTo value;

    /**
     * Internal constructor for the enum.
     *
     * @param value the value of the replication constraint.
     */
    ReplicateTo(Observe.ReplicateTo value) {
        this.value = value;
    }

    /**
     * Returns the actual internal replication representation for the enum.
     *
     * @return the internal replication representation.
     */
    public Observe.ReplicateTo value() {
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
