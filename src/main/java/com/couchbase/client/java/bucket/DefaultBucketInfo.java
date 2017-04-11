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
        } else if (type.equals("ephemeral")) {
            return BucketType.EPHEMERAL;
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
