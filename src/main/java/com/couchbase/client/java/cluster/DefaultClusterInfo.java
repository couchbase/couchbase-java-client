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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.util.features.CouchbaseFeature;
import com.couchbase.client.java.util.features.Version;

/**
 * Default implementation for a {@link ClusterInfo}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class DefaultClusterInfo implements ClusterInfo {

    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(DefaultClusterInfo.class);

    private final JsonObject raw;

    public DefaultClusterInfo(JsonObject raw) {
        this.raw = raw;
    }

    @Override
    public JsonObject raw() {
        return raw;
    }

    @Override
    public boolean checkAvailable(CouchbaseFeature feature) {
        Version minVersion = getMinVersion();
        return feature.isAvailableOn(minVersion);
    }

    @Override
    public Version getMinVersion() {
        List<Version> versions = getAllVersions();
        if (versions.isEmpty()) {
            return Version.NO_VERSION;
        } else if (versions.size() == 1) {
            return versions.get(0);
        } else {
            Version minVersion = versions.get(0);
            for (Version version : versions) {
                if (version.compareTo(minVersion) < 0) {
                    minVersion = version;
                }
            }
            return minVersion;
        }
    }

    @Override
    public List<Version> getAllVersions() {
        try {
            JsonObject raw = raw();
            if (!raw.containsKey("nodes")) {
                return Collections.emptyList();
            }
            List<Version> result = new ArrayList<Version>();
            JsonArray nodes = raw.getArray("nodes");
            for (int i = 0; i < nodes.size(); i++) {
                JsonObject node = nodes.getObject(i);
                if (node.containsKey("version")) {
                    String versionFull = node.getString("version");
                    Version version = Version.parseVersion(versionFull);
                    result.add(version);
                }
            }
            return result;
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Could not obtain cluster list of versions", e);
            }
            return Collections.emptyList();
        }
    }

}
