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

import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.java.document.json.JsonObject;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link BucketInfo}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class DefaultBucketInfo implements BucketInfo {

    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(DefaultBucketInfo.class);

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
    public List<InetAddress> nodeList() {
        List<InetAddress> nodes = new ArrayList<InetAddress>();
        for (Object node : raw.getArray("nodes")) {
            try {
                String hostname = ((JsonObject) node).getString("hostname");
                String[] hostAndPort = hostname.split(":");
                nodes.add(InetAddress.getByName(hostAndPort[0]));
            } catch (Exception ex) {
                LOGGER.warn("Exception while parsing node list on bucket info.", ex);
            }
        }
        return nodes;
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
