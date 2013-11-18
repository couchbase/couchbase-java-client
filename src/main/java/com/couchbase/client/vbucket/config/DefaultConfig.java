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

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.spy.memcached.HashAlgorithm;
import net.spy.memcached.compat.SpyObject;

/**
 * A {@link Config} implementation that represents a "couchbase" bucket config.
 *
 * This {@link Config} implementation is VBucket-aware and allows several
 * operations against a list of nodes and VBuckets. For "memcached" type
 * buckets, see the {@link CacheConfig} implementation.
 */
public class DefaultConfig extends SpyObject implements Config {

  private final HashAlgorithm hashAlgorithm;

  private final int vbucketsCount;

  private final int mask;

  private final int serversCount;

  private final int replicasCount;

  private final List<String> servers;

  private final List<VBucket> vbuckets;

  private final List<URL> couchServers;

  private final Set<String> serversWithVBuckets;

  private final List<String> restEndpoints;

  public DefaultConfig(HashAlgorithm hashAlgorithm, int serversCount,
      int replicasCount, int vbucketsCount, List<String> servers,
      List<VBucket> vbuckets, List<URL> couchServers,
      List<String> restEndpoints) {
    this.hashAlgorithm = hashAlgorithm;
    this.serversCount = serversCount;
    this.replicasCount = replicasCount;
    this.vbucketsCount = vbucketsCount;
    this.mask = vbucketsCount - 1;
    this.servers = servers;
    this.vbuckets = vbuckets;
    this.couchServers = couchServers;
    this.serversWithVBuckets = new HashSet<String>();
    this.restEndpoints = restEndpoints;

    cacheServersWithVBuckets();
  }

  /**
   * Cache all servers with active VBuckets.
   *
   * This methods is called during construction to compute a set of nodes
   * that has active VBuckets. This set is cached and during runtime only
   * needs to be checked upon.
   */
  private void cacheServersWithVBuckets() {
    int serverIndex = 0;
    for (String server : servers) {
      for (VBucket vbucket : vbuckets) {
        if (vbucket.getMaster() == serverIndex) {
          serversWithVBuckets.add(server.split(":")[0]);
          break;
        }
      }
      serverIndex++;
    }

    getLogger().debug("Nodes with active VBuckets: " + serversWithVBuckets);
  }

  @Override
  public int getReplicasCount() {
    return replicasCount;
  }

  @Override
  public int getVbucketsCount() {
    return vbucketsCount;
  }

  @Override
  public int getServersCount() {
    return serversCount;
  }

  @Override
  public String getServer(int serverIndex) {
    return servers.get(serverIndex);
  }

  @Override
  public int getVbucketByKey(String key) {
    int digest = (int) hashAlgorithm.hash(key);
    return digest & mask;
  }

  @Override
  public int getMaster(int vbucketIndex) {
    return vbuckets.get(vbucketIndex).getMaster();
  }

  @Override
  public int getReplica(int vbucketIndex, int replicaIndex) {
    return vbuckets.get(vbucketIndex).getReplica(replicaIndex);
  }

  @Override
  public List<URL> getCouchServers() {
    return couchServers;
  }

  @Override
  public int foundIncorrectMaster(int vbucket, int wrongServer) {
    int mappedServer = this.vbuckets.get(vbucket).getMaster();
    int rv = mappedServer;
    if (mappedServer == wrongServer) {
      rv = (rv + 1) % this.serversCount;
      this.vbuckets.get(vbucket).setMaster((short) rv);
    }
    return rv;
  }

  @Override
  public List<String> getServers() {
    return servers;
  }

  @Override
  public List<VBucket> getVbuckets() {
    return vbuckets;
  }

  /**
   * Compares the given configuration with the current configuration
   * and calculates the differences.
   *
   * Note that if a MEMCACHE type config is used, only the servers are compared
   * because MEMCACHE buckets do not contain vBuckets. If COUCHBASE configs
   * are compared, also the vBucket changes are taken into account.
   *
   * @param config the new config to compare against.
   * @return the differences between the configurations.
   */
  @Override
  public ConfigDifference compareTo(Config config) {
    ConfigDifference difference = new ConfigDifference();

    if (this.serversCount == config.getServersCount()) {
      difference.setSequenceChanged(false);
      for (int i = 0; i < this.serversCount; i++) {
        if (!this.getServer(i).equals(config.getServer(i))) {
          difference.setSequenceChanged(true);
          break;
        }
      }
    } else {
      difference.setSequenceChanged(true);
    }

    if (config.getConfigType().equals(ConfigType.COUCHBASE)
      && this.vbucketsCount == config.getVbucketsCount()) {
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

  @Override
  public HashAlgorithm getHashAlgorithm() {
    return hashAlgorithm;
  }

  @Override
  public List<String> getRestEndpoints() {
    return restEndpoints;
  }

  /**
   * Check if the given node has active VBuckets.
   *
   * Note that the passed in node needs to have the port stripped off, so it
   * can be checked independent of ports.
   *
   * @param node the node to verify.
   * @return if it has active VBuckets or not.
   */
  public boolean nodeHasActiveVBuckets(InetSocketAddress node) {
    boolean result = serversWithVBuckets.contains(node.getHostName());
    if (!result && node.getAddress() != null) {
      result = serversWithVBuckets.contains(node.getAddress().getHostAddress());
    }

    if (!result) {
      getLogger().debug("Given node " + node + " has no active VBuckets.");
    }
    return result;
  }

  @Override
  public ConfigType getConfigType() {
    return ConfigType.COUCHBASE;
  }
}
