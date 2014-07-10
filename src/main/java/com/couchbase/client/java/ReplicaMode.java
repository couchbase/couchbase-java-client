package com.couchbase.client.java;

/**
 * The mode of the replica read to perform.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public enum ReplicaMode {

    /**
     * Asks all replicas and the active node and returns documents from those that respond.
     */
    ALL,

    /**
     * Only asks the first replica for its document.
     */
    FIRST,

    /**
     * Only asks the second replica for its document.
     */
    SECOND,

    /**
     * Only asks the third replica for its document.
     */
    THIRD
}
