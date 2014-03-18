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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import net.spy.memcached.HashAlgorithm;
import net.spy.memcached.HashAlgorithmRegistry;
import net.spy.memcached.compat.SpyObject;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * A {@link ConfigFactory} for creating a {@link Config} out of raw JSON.
 */
public class DefaultConfigFactory extends SpyObject implements ConfigFactory {

  /**
   * Create a {@link Config} from a {@link File}.
   *
   * @param source the source to look it up from.
   * @return the populated {@link Config}.
   */
  @Override
  public Config create(final File source) {
    if (source == null || source.getName().isEmpty()) {
      throw new IllegalArgumentException("Filename is empty.");
    }

    final StringBuilder builder = new StringBuilder();
    try {
      FileInputStream fis = new FileInputStream(source);
      BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
      String str;
      while ((str = reader.readLine()) != null) {
        builder.append(str);
      }
    } catch (IOException e) {
      throw new ConfigParsingException("Exception reading input file: "
        + source, e);
    }
    return create(builder.toString());
  }

  /**
   * Create a {@link Config} from a raw JSON String.
   *
   * @param source the raw JSON string.
   * @return the populated {@link Config}.
   */
  @Override
  public Config create(final String source) {
    try {
      return parseJSON(new JSONObject(source), null);
    } catch (JSONException e) {
      throw new ConfigParsingException("Exception parsing JSON source: "
        + source, e);
    }
  }

  /**
   * Create a {@link Config} from a {@link JSONObject}.
   *
   * @param source the raw {@link JSONObject}.
   * @return the populated {@link Config}.
   */
  @Override
  public Config create(final JSONObject source) {
    try {
      return parseJSON(source, null);
    } catch (JSONException e) {
      throw new ConfigParsingException("Exception parsing JSON data: "
        + source, e);
    }
  }

  @Override
  public Config create(JSONObject source, Config oldConfig) {
    try {
      return parseJSON(source, oldConfig);
    } catch (JSONException e) {
      throw new ConfigParsingException("Exception parsing JSON data: "
        + source, e);
    }
  }

  /**
   * Helper method to identify the type of bucket to parse.
   *
   * @param source the raw {@link JSONObject}.
   * @return the populated {@link Config}.
   * @throws JSONException if parsing the JSON was not successful.
   */
  private Config parseJSON(final JSONObject source, Config oldConfig)
    throws JSONException {
    return source.has("vBucketServerMap")
      ? parseCouchbaseBucketJSON(source, oldConfig) : parseMemcacheBucketJSON(source);
  }

  /**
   * Parse the JSON for a memcache type bucket.
   *
   * @param source the raw {@link JSONObject}.
   * @return the populated {@link Config}.
   * @throws JSONException if parsing the JSON was not successful.
   */
  private Config parseMemcacheBucketJSON(final JSONObject source)
    throws JSONException {
    JSONArray nodes = source.getJSONArray("nodes");

    int amountOfNodes = nodes.length();
    if (amountOfNodes <= 0) {
      throw new ConfigParsingException("Empty nodes list.");
    }

    final List<String> populatedRestEndpoints = populateRestEndpoints(nodes);
    MemcacheConfig config = new MemcacheConfig(amountOfNodes, populatedRestEndpoints);
    populateServersForMemcacheBucket(config, nodes);
    return config;
  }

  /**
   * Helper method to populate the servers into the {@link MemcacheConfig}.
   *
   * @param config the {@link MemcacheConfig} to use.
   * @param nodes the original list of nodes.
   * @throws JSONException if parsing the JSON was not successful.
   */
  private void populateServersForMemcacheBucket(MemcacheConfig config,
    JSONArray nodes) throws JSONException {
    List<String> serverNames = new ArrayList<String>(nodes.length());
    for (int i = 0; i < nodes.length(); i++) {
      JSONObject node = nodes.getJSONObject(i);
      String[] webHostPort = node.getString("hostname").split(":");
      JSONObject portsList = node.getJSONObject("ports");
      int port = portsList.getInt("direct");
      serverNames.add(webHostPort[0] + ":" + port);
    }
    config.setServers(serverNames);
  }


  /**
   * Parse the JSON for a couchbase type bucket.
   *
   * @param source the raw {@link JSONObject}.
   * @param oldConfig an old config where parts can be reused.
   * @return the populated {@link Config}.
   * @throws JSONException if parsing the JSON was not successful.
   */
  private Config parseCouchbaseBucketJSON(JSONObject source, Config oldConfig)
    throws JSONException {
    final JSONObject vBucketServerMap =
      source.getJSONObject("vBucketServerMap");

    final String algorithm = vBucketServerMap.getString("hashAlgorithm");
    HashAlgorithm hashAlgorithm =
      HashAlgorithmRegistry.lookupHashAlgorithm(algorithm);
    if (hashAlgorithm == null) {
      throw new IllegalArgumentException("Unhandled hash algorithm type: "
          + algorithm);
    }

    final int replicasCount = vBucketServerMap.getInt("numReplicas");
    if (replicasCount > VBucket.MAX_REPLICAS) {
      throw new ConfigParsingException("Expected number <= "
          + VBucket.MAX_REPLICAS + " for replicas.");
    }

    final JSONArray servers = vBucketServerMap.getJSONArray("serverList");
    final int serversCount = servers.length();
    if (serversCount <= 0) {
      throw new ConfigParsingException("Empty servers list.");
    }

    final JSONArray viewServers = source.getJSONArray("nodes");
    final int viewServersCount = viewServers.length();
    if (viewServersCount <= 0) {
      throw new ConfigParsingException("Empty view servers list.");
    }

    final JSONArray vBuckets = vBucketServerMap.getJSONArray("vBucketMap");
    final int vBucketsCount = vBuckets.length();
    if (vBucketsCount == 0 || (vBucketsCount & (vBucketsCount - 1)) != 0) {
      throw new ConfigParsingException("Number of vBuckets must be a power of "
        + "two, > 0 and <= " + VBucket.MAX_BUCKETS + " (got " + vBucketsCount
        + ")");
    }

    final boolean hasForwardMap = vBucketServerMap.has("vBucketMapForward");

    final List<String> populatedServers =
      populateServersForCouchbaseBucket(servers);
    final List<VBucket> populatedVBuckets = populateVBuckets(vBuckets, oldConfig);
    final List<URL> populatedViewServers = populateViewServers(viewServers);
    final List<String> populatedRestEndpoints =
      populateRestEndpoints(viewServers);

    return new CouchbaseConfig(hashAlgorithm, serversCount, replicasCount,
      vBucketsCount, populatedServers, populatedVBuckets,
      populatedViewServers, populatedRestEndpoints, hasForwardMap);
  }

  /**
   * Creates a list of REST Endpoints to use.
   *
   * @param nodes the list of nodes in the cluster.
   * @return a list of pouplates endpoints.
   */
  private List<String> populateRestEndpoints(final JSONArray nodes)
    throws JSONException {
    int nodeSize = nodes.length();
    List<String> endpoints = new ArrayList<String>(nodeSize);
    for (int i = 0; i < nodeSize; i++) {
      JSONObject node = nodes.getJSONObject(i);
      if (node.has("hostname")) {
        endpoints.add("http://" + node.getString("hostname") + "/pools");
      }
    }
    return endpoints;
  }

  /**
   * Helper method to create a {@link List} of view server URLs.
   *
   * @param nodes the raw JSON nodes.
   * @return a populated list of URLs.
   * @throws JSONException if parsing the JSON was not successful.
   */
  private List<URL> populateViewServers(JSONArray nodes) throws JSONException {
    int nodeSize = nodes.length();
    List<URL> nodeUrls = new ArrayList<URL>(nodeSize);
    for (int i = 0; i < nodeSize; i++) {
      JSONObject node = nodes.getJSONObject(i);
      if (node.has("couchApiBase")) {
        try {
          nodeUrls.add(new URL(node.getString("couchApiBase")));
        } catch (MalformedURLException e) {
          throw new JSONException("Got bad couchApiBase URL from config");
        }
      }
    }
    return nodeUrls;
  }

  /**
   * Helper method to create a {@link List} of server hostnames.
   *
   * @param nodes the raw JSON nodes.
   * @return a populates list of hostnames.
   * @throws JSONException if parsing the JSON was not successful.
   */
  private List<String> populateServersForCouchbaseBucket(JSONArray nodes)
    throws JSONException {
    int nodeSize = nodes.length();
    List<String> serverNames = new ArrayList<String>(nodeSize);
    for (int i = 0; i < nodeSize; i++) {
      serverNames.add(nodes.getString(i));
    }
    return serverNames;
  }

  /**
   * Helper method to create a {@link List} of {@link VBucket} instances.
   *
   * If an old {@link Config} object is passed in, the code checks if the
   * VBucket is the same and if so just reuses this object instead of creating
   * a new one with the same information.
   *
   * @param source the raw JSON vBucket information.
   * @param oldConfig an optional old config where the VBuckets can be reused.
   * @return a populated list of {@link VBucket} instances.
   * @throws JSONException if parsing the JSON was not successful.
   */
  private List<VBucket> populateVBuckets(JSONArray source, Config oldConfig)
    throws JSONException {
    int numVBuckets = source.length();
    List<VBucket> vBuckets = new ArrayList<VBucket>(numVBuckets);

    List<VBucket> oldvBuckets = null;
    if (oldConfig != null) {
      oldvBuckets = oldConfig.getVbuckets();
    }

    for (int i = 0; i < numVBuckets; i++) {
      JSONArray rows = source.getJSONArray(i);
      short master = (short) rows.getInt(0);
      int replicaSize = rows.length() - 1;
      short[] replicas = new short[replicaSize];
      for (int j = 1; j < rows.length(); j++) {
        replicas[j - 1] = (short) rows.getInt(j);
      }

      if (oldvBuckets != null) {
        VBucket old = oldvBuckets.get(i);
        if (old.getMaster() == master) {
          boolean identicalReplicas = true;
          for (int r = 0; r < replicaSize; r++) {
            if (replicas[r] != old.getReplica(r)) {
              identicalReplicas = false;
              break;
            }
          }
          if (identicalReplicas) {
            vBuckets.add(old);
            continue;
          }
        }
      }

      VBucket vbucket;
      switch (replicaSize) {
        case 0:
          vbucket = new VBucket(master);
          break;
        case 1:
          vbucket = new VBucket(master, replicas[0]);
          break;
        case 2:
          vbucket = new VBucket(master, replicas[0], replicas[1]);
          break;
        case 3:
          vbucket = new VBucket(master, replicas[0], replicas[1], replicas[2]);
          break;
        default:
          throw new IllegalStateException("Not more than 3 replicas supported");
      }
      vBuckets.add(vbucket);
    }
    return vBuckets;
  }
}
