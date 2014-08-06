package com.couchbase.client.java.env;

import com.couchbase.client.core.env.DefaultCoreEnvironment;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DefaultClusterEnvironment extends DefaultCoreEnvironment implements ClusterEnvironment {

    private final ClusterProperties clusterProperties;

    DefaultClusterEnvironment(final ClusterProperties properties) {
       super(properties);
       this.clusterProperties = properties;
    }

    public static DefaultClusterEnvironment create() {
        return new DefaultClusterEnvironment(DynamicClusterProperties.create());
    }

    public static DefaultClusterEnvironment create(final ClusterProperties properties) {
        return new DefaultClusterEnvironment(properties);
    }
}
