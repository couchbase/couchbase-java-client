/**
 * Copyright (C) 2009-2013 Couchbase, Inc.
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

package com.couchbase.client.vbucket.config;

import java.net.URL;
import java.util.List;

import net.spy.memcached.HashAlgorithm;

/**
 * The CacheConfig class represents a configuration object for memcached-type
 * buckets. Unlike couchbase-type buckets, they don't support vbuckets and
 * replicas, so some of the interface methods are not supported.
 */
public class CacheConfig implements Config {

  private int vbucketsCount;

  private final int serversCount;

  private List<String> servers;

  private List<VBucket> vbuckets;

  private final List<String> restEndpoints;

  public CacheConfig(int serversCount, List<String> restEndpoints) {
    this.serversCount = serversCount;
    this.restEndpoints = restEndpoints;
  }

  public List<String> getRestEndpoints() {
    return restEndpoints;
  }

  public int getReplicasCount() {
    throw new UnsupportedOperationException("No replica support for cache"
      + "buckets");
  }

  public int getVbucketsCount() {
    throw new UnsupportedOperationException("No vbucket support for cache"
      + "buckets");
  }

  public int getServersCount() {
    return serversCount;
  }

  public String getServer(int serverIndex) {
    if (serverIndex > servers.size() - 1) {
      throw new IllegalArgumentException(
          "Server index is out of bounds, index = " + serverIndex
          + ", servers count = " + servers.size());
    }
    return servers.get(serverIndex);
  }

  public int getVbucketByKey(String key) {
    throw new UnsupportedOperationException("No vbucket support for cache"
      + "buckets");
  }

  public int getMaster(int vbucketIndex) {
    throw new UnsupportedOperationException("No master/replica support for"
      + "cache buckets");
  }

  public int getReplica(int vbucketIndex, int replicaIndex) {
    throw new UnsupportedOperationException("No replica support for cache"
      + "buckets");
  }

  public int foundIncorrectMaster(int vbucket, int wrongServer) {
    throw new UnsupportedOperationException("No master check for "
      + "cache buckets");
  }

  public void setServers(List<String> newServers) {
    servers = newServers;
  }

  public void setVbuckets(List<VBucket> newVbuckets) {
    vbuckets = newVbuckets;
  }

  public List<String> getServers() {
    return servers;
  }

  public List<VBucket> getVbuckets() {
    return vbuckets;
  }

  public ConfigDifference compareTo(Config config) {
    ConfigDifference difference = new ConfigDifference();

    // Verify the servers are equal in their positions
    if (this.serversCount == config.getServersCount()) {
      difference.setSequenceChanged(false);
      for (int i = 0; i < this.serversCount; i++) {
        if (!this.getServer(i).equals(config.getServer(i))) {
          difference.setSequenceChanged(true);
          break;
        }
      }
    } else {
      // Just say yes
      difference.setSequenceChanged(true);
    }

    // Count the number of vbucket differences
    if (this.vbucketsCount == config.getVbucketsCount()) {
      int vbucketsChanges = 0;
      for (int i = 0; i < this.vbucketsCount; i++) {
        vbucketsChanges += (this.getMaster(i) == config.getMaster(i)) ? 0 : 1;
      }
      difference.setVbucketsChanges(vbucketsChanges);
    } else {
      difference.setVbucketsChanges(-1);
    }

    return difference;
  }

  public HashAlgorithm getHashAlgorithm() {
    throw new UnsupportedOperationException(
      "HashAlgorithm not supported for cache buckets");
  }

  public ConfigType getConfigType() {
    return ConfigType.MEMCACHE;
  }

  @Override
  public List<URL> getCouchServers() {
    throw new UnsupportedOperationException("No couch port for cache buckets");
  }
}
