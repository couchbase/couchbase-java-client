package com.couchbase.client.java.cluster;

import com.couchbase.client.java.bucket.BucketType;

public class DefaultClusterBucketSettings implements ClusterBucketSettings {

    private final String name;
    private final BucketType type;
    private final int quota;
    private final int port;
    private final String password;
    private final int replicas;
    private final boolean indexReplicas;
    private final boolean enableFlush;

    DefaultClusterBucketSettings(Builder builder) {
        name = builder.name();
        type = builder.type();
        quota = builder.quota();
        port = builder.port();
        password = builder.password();
        replicas = builder.replicas();
        indexReplicas = builder.indexReplicas();
        enableFlush = builder.enableFlush();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public BucketType type() {
        return type;
    }

    @Override
    public int quota() {
        return quota;
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public String password() {
        return password;
    }

    @Override
    public int replicas() {
        return replicas;
    }

    @Override
    public boolean indexReplicas() {
        return indexReplicas;
    }

    @Override
    public boolean enableFlush() {
        return enableFlush;
    }

    public static class Builder implements ClusterBucketSettings {

        private String name = "";
        private BucketType type = BucketType.COUCHBASE;
        private int quota = 0;
        private int port = 0;
        private String password = "";
        private int replicas = 0;
        private boolean indexReplicas = false;
        private boolean enableFlush = false;

        @Override
        public String name() {
            return name;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public BucketType type() {
            return type;
        }

        public Builder type(BucketType type) {
            this.type = type;
            return this;
        }

        @Override
        public int quota() {
            return quota;
        }

        public Builder quota(int quota) {
            this.quota = quota;
            return this;
        }

        @Override
        public int port() {
            return port;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        @Override
        public String password() {
            return password;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        @Override
        public int replicas() {
            return replicas;
        }

        public Builder replicas(int replicas) {
            this.replicas = replicas;
            return this;
        }

        @Override
        public boolean indexReplicas() {
            return indexReplicas;
        }

        public Builder indexReplicas(boolean indexReplicas) {
            this.indexReplicas = indexReplicas;
            return this;
        }

        @Override
        public boolean enableFlush() {
            return enableFlush;
        }

        public Builder enableFlush(boolean enableFlush) {
            this.enableFlush = enableFlush;
            return this;
        }

        public DefaultClusterBucketSettings build() {
            return new DefaultClusterBucketSettings(this);
        }

    }

    @Override
    public String toString() {
        return "DefaultClusterBucketSettings{" +
            "name='" + name + '\'' +
            ", type=" + type +
            ", quota=" + quota +
            ", port=" + port +
            ", password='" + password + '\'' +
            ", replicas=" + replicas +
            ", indexReplicas=" + indexReplicas +
            ", enableFlush=" + enableFlush +
            '}';
    }
}
