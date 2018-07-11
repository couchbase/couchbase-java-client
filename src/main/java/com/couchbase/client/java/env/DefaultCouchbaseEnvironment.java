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
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.encryption.CryptoManager;
import com.couchbase.client.java.AsyncCluster;
import com.couchbase.client.java.CouchbaseCluster;

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
    private static final long ANALYTICS_TIMEOUT = TimeUnit.SECONDS.toMillis(75);
    private static final long KV_TIMEOUT = 2500;
    private static final long CONNECT_TIMEOUT = TimeUnit.SECONDS.toMillis(5);
    private static final boolean DNS_SRV_ENABLED = false;
    private static final boolean PROPAGATE_PARENT_SPAN = true;

    private final long managementTimeout;
    private final long queryTimeout;
    private final long viewTimeout;
    private final long searchTimeout;
    private final long analyticsTimeout;
    private final long kvTimeout;
    private final long connectTimeout;
    private final boolean dnsSrvEnabled;
    private final CryptoManager cryptoManager;
    private final boolean propagateParentSpan;

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
        analyticsTimeout = longPropertyOr("analyticsTimeout", builder.analyticsTimeout);
        connectTimeout = longPropertyOr("connectTimeout", builder.connectTimeout);
        dnsSrvEnabled = booleanPropertyOr("dnsSrvEnabled", builder.dnsSrvEnabled);
        propagateParentSpan = booleanPropertyOr("propagateParentSpan", builder.propagateParentSpan);
        cryptoManager = builder.cryptoManager;

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
        if (analyticsTimeout > maxRequestLifetime()) {
            LOGGER.warn("The configured analytics timeout is greater than the maximum request lifetime." +
                "This can lead to falsely cancelled requests.");
        }
        if (searchTimeout > maxRequestLifetime()) {
            LOGGER.warn("The configured search timeout is greater than the maximum request lifetime." +
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

    public static class Builder extends DefaultCoreEnvironment.Builder<Builder> {

        private long managementTimeout = MANAGEMENT_TIMEOUT;
        private long queryTimeout = QUERY_TIMEOUT;
        private long viewTimeout = VIEW_TIMEOUT;
        private long kvTimeout = KV_TIMEOUT;
        private long searchTimeout = SEARCH_TIMEOUT;
        private long analyticsTimeout = ANALYTICS_TIMEOUT;
        private long connectTimeout = CONNECT_TIMEOUT;
        private boolean dnsSrvEnabled = DNS_SRV_ENABLED;
        private boolean propagateParentSpan = PROPAGATE_PARENT_SPAN;
        private CryptoManager cryptoManager;

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

        public Builder analyticsTimeout(long analyticsTimeout) {
            this.analyticsTimeout = analyticsTimeout;
            return this;
        }

        public Builder connectTimeout(long connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder dnsSrvEnabled(boolean dnsSrvEnabled) {
            this.dnsSrvEnabled = dnsSrvEnabled;
            return this;
        }

        public Builder cryptoManager(CryptoManager cryptoManager) {
            this.cryptoManager = cryptoManager;
            return this;
        }

        public Builder propagateParentSpan(boolean propagateParentSpan) {
            this.propagateParentSpan = propagateParentSpan;
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
    public long analyticsTimeout() {
        return analyticsTimeout;
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
    public CryptoManager cryptoManager() { return cryptoManager; }

    @Override
    public boolean propagateParentSpan() {
        return propagateParentSpan;
    }

    @Override
    protected StringBuilder dumpParameters(StringBuilder sb) {
        //first dump core's parameters
        super.dumpParameters(sb);
        //dump java-client specific parameters
        sb.append(", queryTimeout=").append(this.queryTimeout);
        sb.append(", viewTimeout=").append(this.viewTimeout);
        sb.append(", searchTimeout=").append(this.searchTimeout);
        sb.append(", analyticsTimeout=").append(this.analyticsTimeout);
        sb.append(", kvTimeout=").append(this.kvTimeout);
        sb.append(", connectTimeout=").append(this.connectTimeout);
        sb.append(", dnsSrvEnabled=").append(this.dnsSrvEnabled);
        if (this.cryptoManager() != null) {
            sb.append(", cryptoManager=").append(this.cryptoManager.toString());
        }
        sb.append(", propagateParentSpan=").append(this.propagateParentSpan);
        return sb;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CouchbaseEnvironment: {");
        this.dumpParameters(sb).append('}');
        return sb.toString();
    }
}
