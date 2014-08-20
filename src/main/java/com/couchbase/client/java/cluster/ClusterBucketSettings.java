package com.couchbase.client.java.cluster;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public interface ClusterBucketSettings {

    String name();

    ClusterBucketType type();

    int quota();

    int port();

    String password();

    int replicas();

    boolean indexReplicas();

    boolean enableFlush();

}
