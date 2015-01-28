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

import java.util.List;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.util.features.CouchbaseFeature;
import com.couchbase.client.java.util.features.Version;

/**
 * Provides information about a {@link Cluster}.
 *
 * Selected bucket properties are available through explicit getters, the full (raw JSON) response from the server
 * is accessible through the {@link #raw()} method. Note that the response is subject to change across server
 * versions and therefore should be properly checked before being used.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface ClusterInfo {

    /**
     * Provides raw access to the full JSON information from the server.
     *
     * @return the raw JSON cluster info.
     */
    JsonObject raw();

    /**
     * Checks the availability of a specified {@link CouchbaseFeature} on the associated {@link Cluster}.
     *
     * Note that this relies on {@link #getMinVersion()}. If said method returns {@link Version#NO_VERSION} then the
     * feature will be deemed unavailable (this method will return false).
     *
     * @param feature the feature to check for.
     * @return true if minimum node server version is compatible with the feature, false otherwise.
     * @see #getMinVersion()
     */
    public boolean checkAvailable(CouchbaseFeature feature);

    /**
     * Returns the smallest node version (thus oldest version) in the cluster from which this {@link ClusterInfo} was
     * taken. If list of version cannot be obtained then this returns {@link Version#NO_VERSION}.
     *
     * @return the smallest (oldest) server version in the cluster.
     */
    public Version getMinVersion();

    /**
     * Returns the list of {@link Version} obtained from the cluster from which this {@link ClusterInfo} was obtained.
     * In case the versions cannot be obtained, an empty list is returned.
     *
     * @return the list of nodes versions, or an empty list in case of errors.
     */
    public List<Version> getAllVersions();
}
