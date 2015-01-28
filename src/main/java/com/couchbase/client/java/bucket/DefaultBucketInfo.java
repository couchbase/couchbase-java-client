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
package com.couchbase.client.java.bucket;

import com.couchbase.client.java.document.json.JsonObject;

/**
 * Default implementation of {@link BucketInfo}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class DefaultBucketInfo implements BucketInfo {

    private final JsonObject raw;

    DefaultBucketInfo(JsonObject raw) {
        this.raw = raw;
    }

    public static DefaultBucketInfo create(JsonObject raw) {
        return new DefaultBucketInfo(raw);
    }

    @Override
    public String name() {
        return raw.getString("name");
    }

    @Override
    public BucketType type() {
        String type = raw.getString("bucketType");
        if (type.equals("membase")) {
            return BucketType.COUCHBASE;
        } else {
            return BucketType.MEMCACHED;
        }
    }

    @Override
    public int nodeCount() {
        return raw.getArray("nodes").size();
    }

    @Override
    public int replicaCount() {
        return raw.getInt("replicaNumber");
    }

    @Override
    public JsonObject raw() {
        return raw;
    }

    @Override
    public String toString() {
        return "DefaultBucketInfo{" +
            "raw=" + raw +
            '}';
    }
}
