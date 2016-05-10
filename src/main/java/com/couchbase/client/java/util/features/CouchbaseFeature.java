/*
 * Copyright (c) 2014 Couchbase, Inc.
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
    FTS_BETA(4, 5, 0);

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
