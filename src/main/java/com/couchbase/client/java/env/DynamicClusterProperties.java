package com.couchbase.client.java.env;

import com.couchbase.client.core.env.DynamicCoreProperties;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DynamicClusterProperties extends DynamicCoreProperties implements ClusterProperties {

    protected DynamicClusterProperties(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static DynamicClusterProperties create() {
        return new DynamicClusterProperties(new Builder());
    }

    public static class Builder extends DynamicCoreProperties.Builder {

    }
}
