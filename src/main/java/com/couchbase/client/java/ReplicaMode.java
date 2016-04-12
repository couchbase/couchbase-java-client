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

/**
 * Represents the different modes to read from replica nodes.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public enum ReplicaMode {

    /**
     * Get from all replicas and the active node.
     */
    ALL(4),

    /**
     * Get only from the first replica configured for the document.
     */
    FIRST(1),

    /**
     * Get only from the second replica configured for the document.
     */
    SECOND(1),

    /**
     * Get only from the third replica configured for the document.
     */
    THIRD(1);

    private int maxAffectedNodes;

    ReplicaMode(int maxAffectedNodes) {
        this.maxAffectedNodes = maxAffectedNodes;
    }

    public int maxAffectedNodes() {
        return maxAffectedNodes;
    }
}
