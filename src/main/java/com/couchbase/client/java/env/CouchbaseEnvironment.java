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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.env.CoreEnvironment;
import com.couchbase.client.encryption.CryptoManager;
import com.couchbase.client.java.Cluster;

/**
 * The {@link CouchbaseEnvironment} which shares state across {@link Cluster}s.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface CouchbaseEnvironment extends CoreEnvironment {

    /**
     * The default timeout for management operations, set to {@link DefaultCouchbaseEnvironment#MANAGEMENT_TIMEOUT}.
     *
     * @return the default management timeout.
     */
    long managementTimeout();

    /**
     * The default timeout for query operations, set to {@link DefaultCouchbaseEnvironment#QUERY_TIMEOUT}.
     *
     * @return the default query timeout.
     */
    long queryTimeout();

    /**
     * The default timeout for view operations, set to {@link DefaultCouchbaseEnvironment#VIEW_TIMEOUT}.
     *
     * @return the default view timeout.
     */
    long viewTimeout();

    /**
     * The default timeout for search operations, set to {@link DefaultCouchbaseEnvironment#SEARCH_TIMEOUT}.
     *
     * @return the default search timeout.
     */
    long searchTimeout();

    /**
     * The default timeout for analytics operations, set to {@link DefaultCouchbaseEnvironment#ANALYTICS_TIMEOUT}.
     *
     * @return the default analytics timeout.
     */
    long analyticsTimeout();

    /**
     * The default timeout for binary (key/value) operations, set to {@link DefaultCouchbaseEnvironment#KV_TIMEOUT}.
     *
     * @return the default binary timeout.
     */
    long kvTimeout();

    /**
     * The default timeout for connect operations, set to {@link DefaultCouchbaseEnvironment#CONNECT_TIMEOUT}.
     *
     * @return the default connect timeout.
     */
    long connectTimeout();

    /**
     * Returns whether DNS SRV lookup for the bootstrap nodes is enabled or not.
     *
     * @return true if enabled, false otherwise.
     */
    boolean dnsSrvEnabled();

    /**
     * Returns version information on the Couchbase Java SDK client. Version number
     * is in the form MAJOR.MINOR.PATCH, and is the one for the java-client layer.
     *
     * @return the version string for the Java client.
     * @see #clientBuild() for a more specific build information (relevant for tracking
     * the exact version of the code the client was built from)
     * @see #coreVersion() for the same information but relative to the core layer.
     */
    String clientVersion();

    /**
     * Returns build information on the Couchbase Java SDK client. This has a better
     * granularity than {@link #clientVersion()} and thus is more relevant to track
     * the exact version of the code the client was built from.
     *
     * Build information can contain VCS information like commit numbers, tags, etc...
     *
     * @return the build string for the Java client.
     * @see #clientVersion() for more generic version information.
     * @see #coreBuild() for the same information but relative to the core layer.
     */
    String clientBuild();


    /**
     * Returns the crypto manager set.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    CryptoManager cryptoManager();

    /**
     * If set to true, the code will check if a parent span is available and if so
     * use this implicitly as a parent.
     *
     * This is enabled by default for ease of use but can be disabled in case it causes
     * weird effects to parent spans in trace output.
     *
     * @return if a parent span should be propagated automatically.
     */
    boolean propagateParentSpan();
}
