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
package com.couchbase.client.java.env;

import com.couchbase.client.core.env.DefaultCoreEnvironment;
import com.couchbase.client.core.env.KeyValueServiceConfig;
import com.couchbase.client.core.env.QueryServiceConfig;
import com.couchbase.client.core.env.SearchServiceConfig;
import com.couchbase.client.core.env.ViewServiceConfig;
import com.couchbase.client.core.env.WaitStrategyFactory;
import com.couchbase.client.core.env.resources.ShutdownHook;
import com.couchbase.client.core.event.EventBus;
import com.couchbase.client.core.event.consumers.LoggingConsumer;
import com.couchbase.client.core.logging.CouchbaseLogLevel;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.metrics.LatencyMetricsCollectorConfig;
import com.couchbase.client.core.metrics.MetricsCollectorConfig;
import com.couchbase.client.core.node.MemcachedHashingStrategy;
import com.couchbase.client.core.retry.RetryStrategy;
import com.couchbase.client.core.time.Delay;
import com.couchbase.client.deps.com.lmax.disruptor.WaitStrategy;
import com.couchbase.client.deps.io.netty.channel.EventLoopGroup;
import com.couchbase.client.java.AsyncCluster;
import com.couchbase.client.java.CouchbaseCluster;
import rx.Scheduler;

import java.security.KeyStore;
import java.util.Properties;
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

    /**
     * The logger used.
     */
    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(CouchbaseEnvironment.class);

    private static final long MANAGEMENT_TIMEOUT = TimeUnit.SECONDS.toMillis(75);
    private static final long QUERY_TIMEOUT = TimeUnit.SECONDS.toMillis(75);
    private static final long VIEW_TIMEOUT = TimeUnit.SECONDS.toMillis(75);
    private static final long SEARCH_TIMEOUT = TimeUnit.SECONDS.toMillis(75);
    private static final long KV_TIMEOUT = 2500;
    private static final long CONNECT_TIMEOUT = TimeUnit.SECONDS.toMillis(5);
    private static final boolean DNS_SRV_ENABLED = false;

    private final long managementTimeout;
    private final long queryTimeout;
    private final long viewTimeout;
    private final long searchTimeout;
    private final long kvTimeout;
    private final long connectTimeout;
    private final boolean dnsSrvEnabled;

    protected static String CLIENT_VERSION;
    protected static String CLIENT_GIT_VERSION;

    public static String SDK_PACKAGE_NAME_AND_VERSION = "couchbase-java-client";
    public static String SDK_USER_AGENT = SDK_PACKAGE_NAME_AND_VERSION;

    private static final String VERSION_PROPERTIES = "com.couchbase.client.java.properties";

    /**
     * Sets up the package version and user agent.
     *
     * Note that because the class loader loads classes on demand, one class from the package
     * is loaded upfront.
     */
    static {
        try {
            Class<CouchbaseCluster> facadeClass = CouchbaseCluster.class;
            if (facadeClass == null) {
                throw new IllegalStateException("Could not locate ClusterFacade");
            }

            String version = null;
            String gitVersion = null;
            try {
                Properties versionProp = new Properties();
                versionProp.load(DefaultCouchbaseEnvironment.class.getClassLoader().getResourceAsStream(VERSION_PROPERTIES));
                version = versionProp.getProperty("specificationVersion");
                gitVersion = versionProp.getProperty("implementationVersion");
            } catch (Exception e) {
                LOGGER.info("Could not retrieve java-client version properties, defaulting.", e);
            }

            CLIENT_VERSION = version == null ? "unknown" : version;
            CLIENT_GIT_VERSION = gitVersion == null ? "unknown" : gitVersion;

            SDK_PACKAGE_NAME_AND_VERSION = String.format("couchbase-java-client/%s (git: %s, core: %s)",
                    CLIENT_VERSION,
                    CLIENT_GIT_VERSION,
                    CORE_GIT_VERSION);

            SDK_USER_AGENT = String.format("%s (%s/%s %s; %s %s)",
                SDK_PACKAGE_NAME_AND_VERSION,
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"),
                System.getProperty("java.vm.name"),
                System.getProperty("java.runtime.version")
            );
        } catch (Exception ex) {
            LOGGER.info("Could not set up user agent and packages, defaulting.", ex);
        }
    }
    private DefaultCouchbaseEnvironment(final Builder builder) {
       super(builder);

        managementTimeout = longPropertyOr("managementTimeout", builder.managementTimeout);
        queryTimeout = longPropertyOr("queryTimeout", builder.queryTimeout);
        viewTimeout = longPropertyOr("viewTimeout", builder.viewTimeout);
        kvTimeout = longPropertyOr("kvTimeout", builder.kvTimeout);
        searchTimeout = longPropertyOr("searchTimeout", builder.searchTimeout);
        connectTimeout = longPropertyOr("connectTimeout", builder.connectTimeout);
        dnsSrvEnabled = booleanPropertyOr("dnsSrvEnabled", builder.dnsSrvEnabled);

        if (queryTimeout > maxRequestLifetime()) {
            LOGGER.warn("The configured query timeout is greater than the maximum request lifetime. " +
                "This can lead to falsely cancelled requests.");
        }
        if (kvTimeout > maxRequestLifetime()) {
            LOGGER.warn("The configured key/value timeout is greater than the maximum request lifetime." +
                "This can lead to falsely cancelled requests.");
        }
        if (viewTimeout > maxRequestLifetime()) {
            LOGGER.warn("The configured view timeout is greater than the maximum request lifetime." +
                "This can lead to falsely cancelled requests.");
        }
        if (managementTimeout > maxRequestLifetime()) {
            LOGGER.warn("The configured management timeout is greater than the maximum request lifetime." +
                "This can lead to falsely cancelled requests.");
        }
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

    public static class Builder extends DefaultCoreEnvironment.Builder {

        private long managementTimeout = MANAGEMENT_TIMEOUT;
        private long queryTimeout = QUERY_TIMEOUT;
        private long viewTimeout = VIEW_TIMEOUT;
        private long kvTimeout = KV_TIMEOUT;
        private long searchTimeout = SEARCH_TIMEOUT;
        private long connectTimeout = CONNECT_TIMEOUT;
        private boolean dnsSrvEnabled = DNS_SRV_ENABLED;

        public Builder() {
            super();
            //this ensures default values are the ones relative to the client
            //while still making it possible to override.
            packageNameAndVersion(SDK_PACKAGE_NAME_AND_VERSION);
            userAgent(SDK_USER_AGENT);
        }

        public Builder managementTimeout(long managementTimeout) {
            this.managementTimeout = managementTimeout;
            return this;
        }

        public Builder queryTimeout(long queryTimeout) {
            this.queryTimeout = queryTimeout;
            return this;
        }

        public Builder viewTimeout(long viewTimeout) {
            this.viewTimeout = viewTimeout;
            return this;
        }

        public Builder kvTimeout(long kvTimeout) {
            this.kvTimeout = kvTimeout;
            return this;
        }

        public Builder searchTimeout(long searchTimeout) {
            this.searchTimeout = searchTimeout;
            return this;
        }

        public Builder connectTimeout(long connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder disconnectTimeout(long disconnectTimeout) {
            super.disconnectTimeout(disconnectTimeout);
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
        public Builder kvEndpoints(final int kvEndpoints) {
            super.kvEndpoints(kvEndpoints);
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
        public Builder ioPool(EventLoopGroup group, ShutdownHook shutdownHook) {
            super.ioPool(group, shutdownHook);
            return this;
        }

        @Override
        public Builder scheduler(final Scheduler scheduler) {
            super.scheduler(scheduler);
            return this;
        }

        @Override
        public Builder scheduler(Scheduler scheduler, ShutdownHook shutdownHook) {
            super.scheduler(scheduler, shutdownHook);
            return this;
        }

        @Override
        public Builder observeIntervalDelay(Delay observeIntervalDelay) {
            super.observeIntervalDelay(observeIntervalDelay);
            return this;
        }

        @Override
        public Builder reconnectDelay(Delay reconnectDelay) {
            super.reconnectDelay(reconnectDelay);
            return this;
        }

        @Override
        public Builder dcpEnabled(boolean dcpEnabled) {
            super.dcpEnabled(dcpEnabled);
            return this;
        }

        @Override
        public Builder retryDelay(Delay retryDelay) {
            super.retryDelay(retryDelay);
            return this;
        }

        @Override
        public Builder retryStrategy(RetryStrategy retryStrategy) {
            super.retryStrategy(retryStrategy);
            return this;
        }

        @Override
        public Builder maxRequestLifetime(long maxRequestLifetime) {
            super.maxRequestLifetime(maxRequestLifetime);
            return this;
        }

        @Override
        public Builder keepAliveInterval(long keepAliveIntervalMilliseconds) {
            super.keepAliveInterval(keepAliveIntervalMilliseconds);
            return this;
        }

        @Override
        public Builder autoreleaseAfter(long autoreleaseAfter) {
            super.autoreleaseAfter(autoreleaseAfter);
            return this;
        }

        @Override
        public Builder eventBus(EventBus eventBus) {
            super.eventBus(eventBus);
            return this;
        }

        @Override
        public Builder bufferPoolingEnabled(boolean bufferPoolingEnabled) {
            super.bufferPoolingEnabled(bufferPoolingEnabled);
            return this;
        }

        @Override
        public Builder packageNameAndVersion(final String packageNameAndVersion) {
            super.packageNameAndVersion(packageNameAndVersion);
            return this;
        }

        @Override
        public Builder userAgent(final String userAgent) {
            super.userAgent(userAgent);
            return this;
        }

        public Builder dnsSrvEnabled(boolean dnsSrvEnabled) {
            this.dnsSrvEnabled = dnsSrvEnabled;
            return this;
        }

        @Override
        public Builder mutationTokensEnabled(boolean mutationTokensEnabled) {
            super.mutationTokensEnabled(mutationTokensEnabled);
            return this;
        }

        @Override
        public Builder tcpNodelayEnabled(boolean tcpNodelayEnabled) {
            super.tcpNodelayEnabled(tcpNodelayEnabled);
            return this;
        }

        @Override
        public Builder runtimeMetricsCollectorConfig(MetricsCollectorConfig metricsCollectorConfig) {
            super.runtimeMetricsCollectorConfig(metricsCollectorConfig);
            return this;
        }

        @Override
        public Builder networkLatencyMetricsCollectorConfig(LatencyMetricsCollectorConfig metricsCollectorConfig) {
            super.networkLatencyMetricsCollectorConfig(metricsCollectorConfig);
            return this;
        }

        @Override
        public Builder defaultMetricsLoggingConsumer(boolean enabled, CouchbaseLogLevel level, LoggingConsumer.OutputFormat format) {
            super.defaultMetricsLoggingConsumer(enabled, level, format);
            return this;
        }

        @Override
        public Builder defaultMetricsLoggingConsumer(boolean enabled, CouchbaseLogLevel level) {
            super.defaultMetricsLoggingConsumer(enabled, level);
            return this;
        }

        @Override
        public Builder dcpConnectionBufferSize(int dcpConnectionBufferSize) {
            super.dcpConnectionBufferSize(dcpConnectionBufferSize);
            return this;
        }

        @Override
        public Builder dcpConnectionBufferAckThreshold(double dcpConnectionBufferAckThreshold) {
            super.dcpConnectionBufferAckThreshold(dcpConnectionBufferAckThreshold);
            return this;
        }

        @Override
        public Builder socketConnectTimeout(int socketConnectTimeout) {
            super.socketConnectTimeout(socketConnectTimeout);
            return this;
        }

        @Override
        public Builder callbacksOnIoPool(boolean callbacksOnIoPool) {
            super.callbacksOnIoPool(callbacksOnIoPool);
            return this;
        }

        @Override
        public Builder searchEndpoints(int searchEndpoints) {
            super.searchEndpoints(searchEndpoints);
            return this;
        }

        @Override
        public Builder sslKeystore(KeyStore sslKeystore) {
            super.sslKeystore(sslKeystore);
            return this;
        }

        @Override
        public Builder dcpConnectionName(String dcpConnectionName) {
            super.dcpConnectionName(dcpConnectionName);
            return this;
        }

        @Override
        public Builder requestBufferWaitStrategy(WaitStrategyFactory waitStrategy) {
            super.requestBufferWaitStrategy(waitStrategy);
            return this;
        }

        @Override
        public Builder memcachedHashingStrategy(MemcachedHashingStrategy memcachedHashingStrategy) {
            super.memcachedHashingStrategy(memcachedHashingStrategy);
            return this;
        }

        @Override
        public Builder kvIoPool(EventLoopGroup group, ShutdownHook shutdownHook) {
            super.kvIoPool(group, shutdownHook);
            return this;
        }

        @Override
        public Builder viewIoPool(EventLoopGroup group, ShutdownHook shutdownHook) {
            super.viewIoPool(group, shutdownHook);
            return this;
        }

        @Override
        public Builder queryIoPool(EventLoopGroup group, ShutdownHook shutdownHook) {
            super.queryIoPool(group, shutdownHook);
            return this;
        }

        @Override
        public Builder searchIoPool(EventLoopGroup group, ShutdownHook shutdownHook) {
            super.searchIoPool(group, shutdownHook);
            return this;
        }

        @Override
        public Builder keyValueServiceConfig(KeyValueServiceConfig keyValueServiceConfig) {
            super.keyValueServiceConfig(keyValueServiceConfig);
            return this;
        }

        @Override
        public Builder viewServiceConfig(ViewServiceConfig viewServiceConfig) {
            super.viewServiceConfig(viewServiceConfig);
            return this;
        }

        @Override
        public Builder queryServiceConfig(QueryServiceConfig queryServiceConfig) {
            super.queryServiceConfig(queryServiceConfig);
            return this;
        }

        @Override
        public Builder searchServiceConfig(SearchServiceConfig searchServiceConfig) {
            super.searchServiceConfig(searchServiceConfig);
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
    public long searchTimeout() {
        return searchTimeout;
    }

    @Override
    public long kvTimeout() {
        return kvTimeout;
    }

    @Override
    public long connectTimeout() {
        return connectTimeout;
    }

    @Override
    public boolean dnsSrvEnabled() {
        return dnsSrvEnabled;
    }

    @Override
    public String clientVersion() {
        return CLIENT_VERSION;
    }

    @Override
    public String clientBuild() {
        return CLIENT_GIT_VERSION;
    }

    @Override
    protected StringBuilder dumpParameters(StringBuilder sb) {
        //first dump core's parameters
        super.dumpParameters(sb);
        //dump java-client specific parameters
        sb.append(", queryTimeout=").append(this.queryTimeout);
        sb.append(", viewTimeout=").append(this.viewTimeout);
        sb.append(", kvTimeout=").append(this.kvTimeout);
        sb.append(", connectTimeout=").append(this.connectTimeout);
        sb.append(", dnsSrvEnabled=").append(this.dnsSrvEnabled);
        return sb;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CouchbaseEnvironment: {");
        this.dumpParameters(sb).append('}');
        return sb.toString();
    }
}
