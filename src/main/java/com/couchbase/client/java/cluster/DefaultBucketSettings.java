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
package com.couchbase.client.java.cluster;

import com.couchbase.client.java.bucket.BucketType;

public class DefaultBucketSettings implements BucketSettings {

    private final String name;
    private final BucketType type;
    private final int quota;
    private final int port;
    private final String password;
    private final int replicas;
    private final boolean indexReplicas;
    private final boolean enableFlush;

    DefaultBucketSettings(Builder builder) {
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

    public static class Builder implements BucketSettings {

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

        public DefaultBucketSettings build() {
            return new DefaultBucketSettings(this);
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
