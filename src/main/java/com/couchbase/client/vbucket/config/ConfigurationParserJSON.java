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

import com.couchbase.client.vbucket.ConnectionException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.spy.memcached.compat.SpyObject;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * This {@link ConfigurationParser} takes JSON-based configuration information
 * and transforms it into a {@link Bucket}.
 */
public class ConfigurationParserJSON extends SpyObject
  implements ConfigurationParser {

  private final ConfigFactory configFactory = new DefaultConfigFactory();

  /**
   * Parses the /pools URI and returns a map of found pools.
   *
   * @param poolsJson the raw JSON of the pools response.
   * @return a map of found pools.
   * @throws ParseException if the JSON could not be parsed properly.
   * @throws ConnectionException if a non-recoverable connect error happened.
   */
  public Map<String, Pool> parsePools(final String poolsJson)
    throws ParseException {
    final Map<String, Pool> parsedBase = new HashMap<String, Pool>();
    final JSONArray allPools;

    try {
      allPools = new JSONObject(poolsJson).getJSONArray("pools");
    } catch (JSONException e) {
      getLogger().info("Received the following unparsable response: "
        + e.getMessage());
      throw new ConnectionException("Connection URI is either incorrect "
        + "or invalid as it cannot be parsed.");
    }

    for (int i = 0; i < allPools.length(); i++) {
      try {
        final JSONObject currentPool = allPools.getJSONObject(i);
        final String name = currentPool.getString("name");
        if (name == null || name.isEmpty()) {
          throw new ParseException("Pool's name is missing.", 0);
        }
        final URI uri = new URI(currentPool.getString("uri"));
        final URI streamingUri = new URI(currentPool.getString("streamingUri"));
        final Pool pool = new Pool(name, uri, streamingUri);
        parsedBase.put(name, pool);
      } catch (JSONException e) {
        getLogger().error("One of the pool configurations can not be parsed.",
          e);
      } catch (URISyntaxException e) {
        getLogger().error("Server provided an incorrect uri.", e);
      }
    }

    return parsedBase;
  }

  /**
   * Parses a given /pools/{pool} JSON for the buckets URI.
   *
   * @param pool the actual pool object to attach to.
   * @param poolsJson the raw JSON for the pool response.
   * @throws ParseException if the JSON could not be parsed properly.
   */
  public void parsePool(final Pool pool, final String poolsJson)
    throws ParseException {
    try {
      JSONObject buckets = new JSONObject(poolsJson).getJSONObject("buckets");
      URI bucketsUri = new URI(buckets.getString("uri"));
      pool.setBucketsUri(bucketsUri);
    } catch (JSONException e) {
      throw new ParseException(e.getMessage(), 0);
    } catch (URISyntaxException e) {
      throw new ParseException(e.getMessage(), 0);
    }
  }

  /**
   * Parses the /pools/{pool}/buckets URI for a list of contained buckets.
   *
   * @param bucketsJson the raw JSON of the buckets response.
   * @return a map containing all found buckets.
   * @throws ParseException if the JSON could not be parsed properly.
   */
  public Map<String, Bucket> parseBuckets(final String bucketsJson)
    throws ParseException {
    try {
      Map<String, Bucket> bucketsMap = new HashMap<String, Bucket>();
      JSONArray allBuckets = new JSONArray(bucketsJson);

      for (int i = 0; i < allBuckets.length(); i++) {
        JSONObject currentBucket = allBuckets.getJSONObject(i);
        Bucket bucket = parseBucketFromJSON(currentBucket, null);
        bucketsMap.put(bucket.getName(), bucket);
      }

      return bucketsMap;
    } catch (JSONException e) {
      throw new ParseException(e.getMessage(), 0);
    }
  }

  /**
   * Parse a raw bucket config string into a {@link Bucket} configuration.
   *
   * @param bucketJson the raw JSON.
   * @return the parsed configuration.
   * @throws ParseException if the JSON could not be parsed properly.
   */
  public Bucket parseBucket(String bucketJson) throws ParseException {
    try {
      return parseBucketFromJSON(new JSONObject(bucketJson), null);
    } catch (JSONException e) {
      throw new ParseException(e.getMessage(), 0);
    }
  }

  /**
   * Parse a raw bucket config and update an old bucket with the new infos.
   *
   * @param bucketJson the new JSON information.
   * @param currentBucket the current bucket to update.
   * @return the parsed configuration.
   * @throws ParseException if the JSON could not be parsed properly.
   */
  public Bucket updateBucket(String bucketJson, Bucket currentBucket)
    throws ParseException {
    try {
      return parseBucketFromJSON(new JSONObject(bucketJson), currentBucket);
    } catch (JSONException e) {
      throw new ParseException(e.getMessage(), 0);
    }
  }

  /**
   * Helper method to create a {@link Bucket} config from JSON.
   *
   * Note that a new bucket is currently always returned, the optional bucket
   * that can be passed in is used to reuse already existing {@link VBucket}
   * to reduce GC pressure in rebalance phases.
   *
   * @param bucketJson the input as a {@link JSONObject}.
   * @param current the optional current bucket.
   * @return a parsed {@link Bucket} configuration.
   * @throws ParseException if the JSON could not be parsed properly.
   */
  private Bucket parseBucketFromJSON(JSONObject bucketJson, Bucket current)
    throws ParseException {
    try {
      String bucketName = bucketJson.getString("name");
      URI streamingUri = new URI(bucketJson.getString("streamingUri"));
      Config currentConfig = null;
      if (current != null) {
        currentConfig = current.getConfig();
      }
      Config config = configFactory.create(bucketJson, currentConfig);

      List<Node> nodes = new ArrayList<Node>();
      JSONArray allNodes = bucketJson.getJSONArray("nodes");
      for (int i = 0; i < allNodes.length(); i++) {
        JSONObject currentNode = allNodes.getJSONObject(i);
        // TODO: remove status field completely, not needed.
        //Status status = parseNodeStatus(currentNode.getString("status"));
        Status status = Status.healthy;
        String hostname = currentNode.getString("hostname");
        Map<Port, String> ports = extractPorts(
          currentNode.getJSONObject("ports"));
        nodes.add(new Node(status, hostname, ports));
      }
      return new Bucket(bucketName, config, streamingUri, nodes);
    } catch (JSONException e) {
      throw new ParseException(e.getMessage(), 0);
    } catch (URISyntaxException e) {
      throw new ParseException(e.getMessage(), 0);
    }
  }

  /**
   * Helper method to parse a node {@link Status} out of the raw response.
   *
   * @param status the status to parse.
   * @return the parsed status enum value.
   */
  private Status parseNodeStatus(String status) {
    if (status == null || status.isEmpty()) {
      return null;
    }

    try {
      return Status.valueOf(status);
    } catch (IllegalArgumentException e) {
      getLogger().error("Unknown status value: " + status);
      return null;
    }
  }

  /**
   * Helper method to extract a map of node ports from a {@link JSONObject}.
   *
   * @param portsJson the port information.
   * @return the extracted port map.
   * @throws JSONException if the JSON could not be parsed as expected.
   */
  private Map<Port, String> extractPorts(JSONObject portsJson)
    throws JSONException {
    Map<Port, String> ports = new HashMap<Port, String>();
    for (Port port : Port.values()) {
      String portValue = portsJson.getString(port.toString());
      if (portValue == null || portValue.isEmpty()) {
        continue;
      }
      ports.put(port, portValue);
    }
    return ports;
  }

}
