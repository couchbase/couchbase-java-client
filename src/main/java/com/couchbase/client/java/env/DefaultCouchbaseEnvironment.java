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
package com.couchbase.client.java.env;

import com.couchbase.client.core.env.DefaultCoreEnvironment;
import com.couchbase.client.deps.io.netty.channel.EventLoopGroup;
import com.couchbase.client.java.AsyncCluster;
import rx.Scheduler;

import java.util.concurrent.TimeUnit;

/**
 * The default implementation of a {@link CouchbaseEnvironment}.
 *
 * This environment is intended to be reused and passed in across {@link AsyncCluster} instances. It is stateful and needs
 * to be shut down manually if it was passed in by the user. Some threads it manages are non-daemon threads.
 *
 * Default settings can be customized through the {@link Builder} or through the setting of system properties. Latter
 * ones take always precedence and can be used to override builder settings at runtime too.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class DefaultCouchbaseEnvironment extends DefaultCoreEnvironment implements CouchbaseEnvironment {

    private static final String NAMESPACE = "com.couchbase.";

    private static final long MANAGEMENT_TIMEOUT = TimeUnit.SECONDS.toMillis(75);
    private static final long QUERY_TIMEOUT = TimeUnit.SECONDS.toMillis(75);
    private static final long VIEW_TIMEOUT = TimeUnit.SECONDS.toMillis(75);
    private static final long BINARY_TIMEOUT = 2500;
    private static final long CONNECT_TIMEOUT = TimeUnit.SECONDS.toMillis(5);
    private static final long DISCONNECT_TIMEOUT = TimeUnit.SECONDS.toMillis(5);

    private final long managementTimeout;
    private final long queryTimeout;
    private final long viewTimeout;
    private final long binaryTimeout;
    private final long connectTimeout;
    private final long disconnectTimeout;

    private DefaultCouchbaseEnvironment(final Builder builder) {
       super(builder);

        managementTimeout = longPropertyOr("managementTimeout", builder.managementTimeout());
        queryTimeout = longPropertyOr("queryTimeout", builder.queryTimeout());
        viewTimeout = longPropertyOr("viewTimeout", builder.viewTimeout());
        binaryTimeout = longPropertyOr("binaryTimeout", builder.binaryTimeout());
        connectTimeout = longPropertyOr("connectTimeout", builder.connectTimeout());
        disconnectTimeout = longPropertyOr("disconnectTimeout", builder.disconnectTimeout());
    }

    private static long longPropertyOr(String path, long def) {
        String found = System.getProperty(NAMESPACE + path);
        if (found == null) {
            return def;
        }
        return Integer.parseInt(found);
    }

    /**
     * Creates a {@link CouchbaseEnvironment} with default settings applied.
     *
     * @return a {@link DefaultCouchbaseEnvironment} with default settings.
     */
    public static DefaultCouchbaseEnvironment create() {
        return builder().build();
    }

    /**
     * Returns the {@link Builder} to customize environment settings.
     *
     * @return the {@link Builder}.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends DefaultCoreEnvironment.Builder implements CouchbaseEnvironment {

        private long managementTimeout = MANAGEMENT_TIMEOUT;
        private long queryTimeout = QUERY_TIMEOUT;
        private long viewTimeout = VIEW_TIMEOUT;
        private long binaryTimeout = BINARY_TIMEOUT;
        private long connectTimeout = CONNECT_TIMEOUT;
        private long disconnectTimeout = DISCONNECT_TIMEOUT;

        @Override
        public long managementTimeout() {
            return managementTimeout;
        }

        public Builder managementTimeout(long managementTimeout) {
            this.managementTimeout = managementTimeout;
            return this;
        }

        @Override
        public long queryTimeout() {
            return queryTimeout;
        }

        public Builder queryTimeout(long queryTimeout) {
            this.queryTimeout = queryTimeout;
            return this;
        }

        @Override
        public long viewTimeout() {
            return viewTimeout;
        }

        public Builder viewTimeout(long viewTimeout) {
            this.viewTimeout = viewTimeout;
            return this;
        }

        @Override
        public long binaryTimeout() {
            return binaryTimeout;
        }

        public Builder binaryTimeout(long binaryTimeout) {
            this.binaryTimeout = binaryTimeout;
            return this;
        }

        @Override
        public long connectTimeout() {
            return connectTimeout;
        }

        public Builder connectTimeout(long connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        @Override
        public long disconnectTimeout() {
            return disconnectTimeout;
        }

        public Builder disconnectTimeout(long disconnectTimeout) {
            this.disconnectTimeout = disconnectTimeout;
            return this;
        }

        @Override
        public Builder sslEnabled(final boolean sslEnabled) {
            super.sslEnabled(sslEnabled);
            return this;
        }

        @Override
        public Builder sslKeystoreFile(final String sslKeystoreFile) {
            super.sslKeystoreFile(sslKeystoreFile);
            return this;
        }

        @Override
        public Builder sslKeystorePassword(final String sslKeystorePassword) {
            super.sslKeystorePassword(sslKeystorePassword);
            return this;
        }

        @Override
        public Builder queryEnabled(final boolean queryEnabled) {
            super.queryEnabled(queryEnabled);
            return this;
        }

        @Override
        public Builder queryPort(final int queryPort) {
            super.queryPort(queryPort);
            return this;
        }

        @Override
        public Builder bootstrapHttpEnabled(final boolean bootstrapHttpEnabled) {
            super.bootstrapHttpEnabled(bootstrapHttpEnabled);
            return this;
        }

        @Override
        public Builder bootstrapCarrierEnabled(final boolean bootstrapCarrierEnabled) {
            super.bootstrapCarrierEnabled(bootstrapCarrierEnabled);
            return this;
        }

        @Override
        public Builder bootstrapHttpDirectPort(final int bootstrapHttpDirectPort) {
            super.bootstrapHttpDirectPort(bootstrapHttpDirectPort);
            return this;
        }

        @Override
        public Builder bootstrapHttpSslPort(final int bootstrapHttpSslPort) {
            super.bootstrapHttpSslPort(bootstrapHttpSslPort);
            return this;
        }

        @Override
        public Builder bootstrapCarrierDirectPort(final int bootstrapCarrierDirectPort) {
            super.bootstrapCarrierDirectPort(bootstrapCarrierDirectPort);
            return this;
        }

        @Override
        public Builder bootstrapCarrierSslPort(final int bootstrapCarrierSslPort) {
            super.bootstrapCarrierSslPort(bootstrapCarrierSslPort);
            return this;
        }

        @Override
        public Builder ioPoolSize(final int ioPoolSize) {
            super.ioPoolSize(ioPoolSize);
            return this;
        }

        @Override
        public Builder computationPoolSize(final int computationPoolSize) {
            super.computationPoolSize(computationPoolSize);
            return this;
        }

        @Override
        public Builder requestBufferSize(final int requestBufferSize) {
            super.requestBufferSize(requestBufferSize);
            return this;
        }

        @Override
        public Builder responseBufferSize(final int responseBufferSize) {
            super.responseBufferSize(responseBufferSize);
            return this;
        }

        @Override
        public Builder binaryEndpoints(final int binaryServiceEndpoints) {
            super.binaryEndpoints(binaryServiceEndpoints);
            return this;
        }

        @Override
        public Builder viewEndpoints(final int viewServiceEndpoints) {
            super.viewEndpoints(viewServiceEndpoints);
            return this;
        }

        @Override
        public Builder queryEndpoints(final int queryServiceEndpoints) {
            super.queryEndpoints(queryServiceEndpoints);
            return this;
        }

        @Override
        public Builder ioPool(final EventLoopGroup group) {
            super.ioPool(group);
            return this;
        }

        @Override
        public Builder scheduler(final Scheduler scheduler) {
            super.scheduler(scheduler);
            return this;
        }

        @Override
        public DefaultCouchbaseEnvironment build() {
            return new DefaultCouchbaseEnvironment(this);
        }
    }

    @Override
    public long managementTimeout() {
        return managementTimeout;
    }

    @Override
    public long queryTimeout() {
        return queryTimeout;
    }

    @Override
    public long viewTimeout() {
        return viewTimeout;
    }

    @Override
    public long binaryTimeout() {
        return binaryTimeout;
    }

    @Override
    public long connectTimeout() {
        return connectTimeout;
    }

    @Override
    public long disconnectTimeout() {
        return disconnectTimeout;
    }
}
