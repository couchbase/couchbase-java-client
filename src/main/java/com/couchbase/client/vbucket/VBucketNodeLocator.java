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

package com.couchbase.client.vbucket;

import com.couchbase.client.vbucket.config.Config;
import com.couchbase.client.vbucket.config.ConfigDifference;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import net.spy.memcached.MemcachedNode;
import net.spy.memcached.NodeLocator;
import net.spy.memcached.compat.SpyObject;

/**
 * Implementation of the {@link NodeLocator} interface that contains vbucket
 * hashing methods.
 */
public class VBucketNodeLocator extends SpyObject implements NodeLocator {

  private final AtomicReference<TotalConfig> fullConfig;

  /**
   * Construct a VBucketNodeLocator over the given JSON configuration string.
   *
   * @param nodes
   * @param jsonConfig
   */
  public VBucketNodeLocator(List<MemcachedNode> nodes, Config jsonConfig) {
    super();
    fullConfig = new AtomicReference<TotalConfig>();
    fullConfig.set(new TotalConfig(jsonConfig,
            fillNodesEntries(jsonConfig, nodes)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MemcachedNode getPrimary(String k) {
    TotalConfig totConfig = fullConfig.get();
    Config config = totConfig.getConfig();
    Map<String, MemcachedNode> nodesMap = totConfig.getNodesMap();
    int vbucket = config.getVbucketByKey(k);
    int serverNumber = config.getMaster(vbucket);

    if(serverNumber == -1) {
      getLogger().warn("The key "+ k +" pointed to vbucket "+ vbucket
        + ", for which no server is responsible in the cluster map (-1). This "
        + "can be an indication that either no replica is defined for a "
        + "failed server or more nodes have been failed over than replicas "
        + "defined.");
      return null;
    }

    String server = config.getServer(serverNumber);
    // choose appropriate MemcachedNode according to config data
    MemcachedNode pNode = nodesMap.get(server);
    if (pNode == null) {
      getLogger().error("The node locator does not have a primary for key"
        + " %s.  Wanted vbucket %s which should be on server %s.", k,
        vbucket, server);
      getLogger().error("List of nodes has %s entries:", nodesMap.size());
      Set<String> keySet = nodesMap.keySet();
      Iterator<String> iterator = keySet.iterator();
      while (iterator.hasNext()) {
        String anode = iterator.next();
        getLogger().error("MemcachedNode for %s is %s", anode,
          nodesMap.get(anode));
      }
      Collection<MemcachedNode> nodes = nodesMap.values();
      for (MemcachedNode node : nodes) {
        getLogger().error(node);
      }
    }
    assert (pNode != null);
    return pNode;
  }

  /**
   * Return a replica node for the given key and replica index.
   *
   * Based on the ReplicaIndex ID given, this method calculates the
   * replica node. It works similar to the getMaster method, but has the
   * additional capability to find the node based on the given replica
   * index.
   *
   * @param key the key to find the node for.
   * @param index the Nth replica number
   * @return the node where the given replica exists
   * @throws RuntimeException when no replica is defined for the given key
   */
  public MemcachedNode getReplica(String key, int index) {
    TotalConfig totConfig = fullConfig.get();
    Config config = totConfig.getConfig();
    Map<String, MemcachedNode> nodesMap = totConfig.getNodesMap();
    int vbucket = config.getVbucketByKey(key);
    int serverNumber = config.getReplica(vbucket, index);

    if(serverNumber == -1) {
      throw new RuntimeException("The key " + key + " pointed to vbucket "
        + vbucket + ", for which no server is responsible in the cluster map."
        + "This can be an indication that either no replica is defined for a "
        + "failed server or more nodes have been failed over than replicas "
        + "defined.");
    }

    String server = config.getServer(serverNumber);
    MemcachedNode pNode = nodesMap.get(server);
    return pNode;
  }

  public MemcachedNode getServerByIndex(int k) {
    TotalConfig totConfig = fullConfig.get();
    Config config = totConfig.getConfig();
    Map<String, MemcachedNode> nodesMap = totConfig.getNodesMap();

    String server = config.getServer(k);
    // choose appropriate MemcachedNode according to config data
    return nodesMap.get(server);
  }
  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<MemcachedNode> getSequence(String k) {
    return new NullIterator<MemcachedNode>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<MemcachedNode> getAll() {
    Map<String, MemcachedNode> nodesMap = fullConfig.get().getNodesMap();
    return nodesMap.values();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NodeLocator getReadonlyCopy() {
    return this;
  }

  @Override
  public void updateLocator(List<MemcachedNode> nodes) {
    throw new UnsupportedOperationException("Must be updated with a config");
  }

  public void updateLocator(final Collection<MemcachedNode> nodes,
      final Config newconf) {
    Config current = fullConfig.get().getConfig();

    ConfigDifference compareTo = current.compareTo(newconf);

    if (compareTo.isSequenceChanged() || compareTo.getVbucketsChanges() > 0
      || current.getCouchServers().size() != newconf.getCouchServers().size()) {
      getLogger().debug("Updating configuration, received updated configuration"
        + " with significant changes.");
      fullConfig.set(new TotalConfig(newconf,
              fillNodesEntries(newconf, nodes)));
    } else {
      getLogger().debug("Received updated configuration with insignificant "
        + "changes.");
    }
  }

  /**
   * Returns a vbucket index for the given key.
   *
   * @param key the key
   * @return vbucket index
   */
  public int getVBucketIndex(String key) {
    Config config = fullConfig.get().getConfig();
    return config.getVbucketByKey(key);
  }

  private Map<String, MemcachedNode> fillNodesEntries(
      Config newConfig, final Collection<MemcachedNode> nodes) {
    HashMap<String, MemcachedNode> vbnodesMap =
        new HashMap<String, MemcachedNode>();
    getLogger().debug("Updating nodesMap in VBucketNodeLocator.");
    for (String server : newConfig.getServers()) {
      vbnodesMap.put(server, null);
    }

    for (MemcachedNode node : nodes) {
      InetSocketAddress addr = (InetSocketAddress) node.getSocketAddress();
      String address = addr.getAddress().getHostName() + ":" + addr.getPort();
      String hostname = addr.getAddress().getHostAddress() + ":"
        + addr.getPort();

      if (vbnodesMap.containsKey(address)) {
        vbnodesMap.put(address, node);
        getLogger().debug("Adding node with address %s.",
          address);
        getLogger().debug("Node added is %s.", node);
      } else if (vbnodesMap.containsKey(hostname)) {
        vbnodesMap.put(hostname, node);
        getLogger().debug("Adding node with hostname %s.",
          hostname);
        getLogger().debug("Node added is %s.", node);
      }
    }
    // Iterate over the map and check for entries not populated
    for (Map.Entry<String, MemcachedNode> entry : vbnodesMap.entrySet()) {
      if (entry.getValue() == null) {
        getLogger().error("Critical reconfiguration error: "
            + "Server list from Configuration and Nodes "
            + "are out of synch. causing %s to be removed",
                entry.getKey());
        vbnodesMap.remove(entry.getKey());
      }
    }
    return Collections.unmodifiableMap(vbnodesMap);
  }

  /**
   * Method returns the node that is not contained in the specified collection
   * of the failed nodes.
   *
   * @param k the key
   * @param notMyVbucketNodes a collection of the nodes are excluded
   * @return The first MemcachedNode which meets requirements
   */
  public MemcachedNode getAlternative(String k,
      Collection<MemcachedNode> notMyVbucketNodes) {
    // it's safe to only copy the map here, only removing references found to be
    // incorrect, and trying remaining
    Map<String, MemcachedNode> nodesMap =
        new HashMap<String, MemcachedNode>(fullConfig.get().getNodesMap());
    Collection<MemcachedNode> nodes = nodesMap.values();
    nodes.removeAll(notMyVbucketNodes);
    if (nodes.isEmpty()) {
      return null;
    } else {
      return nodes.iterator().next();
    }
  }

  private static class TotalConfig {
    private Config config;
    private Map<String, MemcachedNode> nodesMap;

    public TotalConfig(Config newConfig, Map<String, MemcachedNode> newMap) {
      config = newConfig;
      nodesMap = Collections.unmodifiableMap(newMap);
    }

    protected Config getConfig() {
      return config;
    }

    protected Map<String, MemcachedNode> getNodesMap() {
      return nodesMap;
    }
  }

  private static class NullIterator<E> implements Iterator<MemcachedNode> {

    public boolean hasNext() {
      return false;
    }

    public MemcachedNode next() {
      throw new NoSuchElementException(
          "VBucketNodeLocators have no alternate nodes.");
    }

    public void remove() {
      throw new UnsupportedOperationException(
          "VBucketNodeLocators have no alternate nodes; cannot remove.");
    }
  }
}
