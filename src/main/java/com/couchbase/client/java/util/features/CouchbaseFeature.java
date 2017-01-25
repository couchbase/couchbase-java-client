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
package com.couchbase.client.java.util.features;

/**
 * Enumeration of all Couchbase Features supported by this SDK.
 *
 * @author Simon Baslé
 * @since 2.1.0
 */
public enum CouchbaseFeature {

    KV(1, 8, 0),
    VIEW(2, 0, 0),
    CCCP(2, 5, 0),
    SSL(3, 0, 0),
    DCP(3, 0, 0),
    N1QL(4, 0, 0),
    SPATIAL_VIEW(4, 0, 0),
    SUBDOC(4, 5, 0),

    /**
     * @deprecated FTS is still in BETA in 4.5.0, likely to get major changes when switching to GA.
     */
    @Deprecated
    FTS_BETA(4, 5, 0),

    XATTR(5,0,0);

    private final Version availableFrom;

    CouchbaseFeature(int major, int minor, int patch) {
        this.availableFrom = new Version(major, minor, patch);
    }

    /**
     * Checks if this feature is available on the provided server version.
     *
     * @param serverVersion the server side version to check against
     * @return true if this feature is available on the given version, false otherwise.
     */
    public boolean isAvailableOn(Version serverVersion) {
        return serverVersion.compareTo(availableFrom) >= 0;
    }
}
