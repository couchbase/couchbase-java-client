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
