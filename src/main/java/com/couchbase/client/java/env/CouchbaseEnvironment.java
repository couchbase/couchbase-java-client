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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.env.CoreEnvironment;
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
     * The default timeout for disconnect operations, set to {@link DefaultCouchbaseEnvironment#DISCONNECT_TIMEOUT}.
     *
     * @return the default disconnect timeout.
     */
    long disconnectTimeout();

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
}
