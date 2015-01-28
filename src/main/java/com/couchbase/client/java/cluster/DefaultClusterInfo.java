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
