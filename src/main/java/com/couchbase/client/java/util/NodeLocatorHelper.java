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
package com.couchbase.client.java.util;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.config.BucketConfig;
import com.couchbase.client.core.config.ClusterConfig;
import com.couchbase.client.core.config.ConfigurationProvider;
import com.couchbase.client.core.config.CouchbaseBucketConfig;
import com.couchbase.client.core.config.MemcachedBucketConfig;
import com.couchbase.client.core.config.NodeInfo;
import com.couchbase.client.core.message.internal.GetConfigProviderRequest;
import com.couchbase.client.core.message.internal.GetConfigProviderResponse;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.Bucket;
import rx.functions.Action1;
import rx.functions.Func1;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32;

/**
 * Helper class to provide direct access on how document IDs are mapped onto nodes.
 *
 * @author Michael Nitschinger
 * @since 2.1.0
 */
// TODO: lots of logic is duplicated between this implementation and the actual key value locator of the core
// TODO: package. In a future version, the logic should be refactored into the config itself so that the redundancy is
// TODO: removed.
@InterfaceStability.Uncommitted
@InterfaceAudience.Public
public class NodeLocatorHelper {

    private final ConfigurationProvider configProvider;
    private final AtomicReference<BucketConfig> bucketConfig;

    private NodeLocatorHelper(final Bucket bucket) {
        configProvider = bucket
            .core()
            .<GetConfigProviderResponse>send(new GetConfigProviderRequest())
            .toBlocking()
            .single()
            .provider();

        bucketConfig = new AtomicReference<BucketConfig>(configProvider.config().bucketConfig(bucket.name()));

        configProvider
            .configs()
            .filter(new Func1<ClusterConfig, Boolean>() {
                @Override
                public Boolean call(ClusterConfig clusterConfig) {
                    return clusterConfig.hasBucket(bucket.name());
                }
            }).subscribe(new Action1<ClusterConfig>() {
                @Override
                public void call(ClusterConfig config) {
                    bucketConfig.set(config.bucketConfig(bucket.name()));
                }
            });
    }

    /**
     * Creates a new {@link NodeLocatorHelper}, mapped on to the given {@link Bucket}.
     *
     * @param bucket the scoped bucket.
     * @return the created locator.
     */
    public static NodeLocatorHelper create(final Bucket bucket) {
        return new NodeLocatorHelper(bucket);
    }

    /**
     * Returns the target active node {@link InetAddress} for a given document ID on the bucket.
     *
     * @param id the document id to convert.
     * @return the node for the given document id.
     */
    public InetAddress activeNodeForId(final String id) {
        BucketConfig config = bucketConfig.get();

        if (config instanceof CouchbaseBucketConfig) {
            return nodeForIdOnCouchbaseBucket(id, (CouchbaseBucketConfig) config);
        } else if (config instanceof MemcachedBucketConfig) {
            return nodeForIdOnMemcachedBucket(id, (MemcachedBucketConfig) config);
        } else {
            throw new UnsupportedOperationException("Bucket type not supported: " + config.getClass().getName());
        }
    }

    /**
     * Returns all target replica nodes {@link InetAddress} for a given document ID on the bucket.
     *
     * @param id the document id to convert.
     * @return the node for the given document id.
     */
    public List<InetAddress> replicaNodesForId(final String id) {
        BucketConfig config = bucketConfig.get();

        if (config instanceof CouchbaseBucketConfig) {
            CouchbaseBucketConfig cbc = (CouchbaseBucketConfig) config;
            List<InetAddress> replicas = new ArrayList<InetAddress>();
            for (int i = 1; i <= cbc.numberOfReplicas(); i++) {
                replicas.add(replicaNodeForId(id, i));
            }
            return replicas;
        } else {
            throw new UnsupportedOperationException("Bucket type not supported: " + config.getClass().getName());
        }
    }

    /**
     * Returns the target replica node {@link InetAddress} for a given document ID and replica number on the bucket.
     *
     * @param id the document id to convert.
     * @param replicaNum the replica number.
     * @return the node for the given document id.
     */
    public InetAddress replicaNodeForId(final String id, int replicaNum) {
        if (replicaNum < 1 || replicaNum > 3) {
            throw new IllegalArgumentException("Replica number must be between 1 and 3.");
        }

        BucketConfig config = bucketConfig.get();

        if (config instanceof CouchbaseBucketConfig) {
            CouchbaseBucketConfig cbc = (CouchbaseBucketConfig) config;
            int partitionId = (int) hashId(id) & cbc.numberOfPartitions() - 1;
            int nodeId = cbc.nodeIndexForReplica(partitionId, replicaNum - 1, false);
            if (nodeId == -1) {
                throw new IllegalStateException("No partition assigned to node for Document ID: " + id);
            }
            if (nodeId == -2) {
                throw new IllegalStateException("Replica not configured for this bucket.");
            }
            try {
                return InetAddress.getByName(cbc.nodeAtIndex(nodeId).hostname().address());
            } catch (UnknownHostException e) {
                throw new IllegalStateException(e);
            }
        }  else {
            throw new UnsupportedOperationException("Bucket type not supported: " + config.getClass().getName());
        }
    }

    /**
     * Returns all nodes known in the current config.
     *
     * @return all currently known nodes.
     */
    public List<InetAddress> nodes() {
        List<InetAddress> allNodes = new ArrayList<InetAddress>();
        BucketConfig config = bucketConfig.get();
        for (NodeInfo nodeInfo : config.nodes()) {
            try {
                allNodes.add(InetAddress.getByName(nodeInfo.hostname().address()));
            } catch (UnknownHostException e) {
                throw new IllegalStateException(e);
            }
        }
        return allNodes;
    }

    private static InetAddress nodeForIdOnCouchbaseBucket(final String id, final CouchbaseBucketConfig config) {
        int partitionId = (int) hashId(id) & config.numberOfPartitions() - 1;
        int nodeId = config.nodeIndexForMaster(partitionId, false);
        if (nodeId == -1) {
            throw new IllegalStateException("No partition assigned to node for Document ID: " + id);
        }
        try {
            return InetAddress.getByName(config.nodeAtIndex(nodeId).hostname().address());
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    private static InetAddress nodeForIdOnMemcachedBucket(final String id, final MemcachedBucketConfig config) {
        long hash = ketamaHash(id);
        if (!config.ketamaNodes().containsKey(hash)) {
            SortedMap<Long, NodeInfo> tailMap = config.ketamaNodes().tailMap(hash);
            if (tailMap.isEmpty()) {
                hash = config.ketamaNodes().firstKey();
            } else {
                hash = tailMap.firstKey();
            }
        }
        try {
            return InetAddress.getByName(config.ketamaNodes().get(hash).hostname().address());
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    private static long hashId(String id) {
        CRC32 crc32 = new CRC32();
        try {
            crc32.update(id.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return (crc32.getValue() >> 16) & 0x7fff;
    }

    private static long ketamaHash(final String key) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(key.getBytes(CharsetUtil.UTF_8));
            byte[] digest = md5.digest();
            long rv = ((long) (digest[3] & 0xFF) << 24)
                | ((long) (digest[2] & 0xFF) << 16)
                | ((long) (digest[1] & 0xFF) << 8)
                | (digest[0] & 0xFF);
            return rv & 0xffffffffL;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not encode ketama hash.", e);
        }
    }

}
