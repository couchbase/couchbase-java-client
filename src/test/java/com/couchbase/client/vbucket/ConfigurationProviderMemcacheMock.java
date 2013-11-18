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

import com.couchbase.client.vbucket.config.Bucket;
import com.couchbase.client.vbucket.config.CacheConfig;
import com.couchbase.client.vbucket.config.Node;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.spy.memcached.TestConfig;

/**
 * Implements a stub configuration provider for testing memcache buckets.
 */
public class ConfigurationProviderMemcacheMock
  implements ConfigurationProvider {

  private final List<String> nodeList;
  public boolean baseListUpdated;

  public ConfigurationProviderMemcacheMock(List<String> nodeList) {
    this.nodeList = nodeList;
    baseListUpdated = false;
  }

  public ConfigurationProviderMemcacheMock() {
    this(Arrays.asList(TestConfig.IPV4_ADDR+":8091"));
  }

  public Bucket getBucketConfiguration(String bucketname) {

    String uri = "http://"+TestConfig.IPV4_ADDR+":8091";
    URI streamingURI = URI.create(uri);
    List<String> restEndpoints = Arrays.asList(uri + "/pools");
    CacheConfig config = new CacheConfig(1, restEndpoints);
    config.setServers(nodeList);

    List<Node> nodes = new ArrayList<Node>();

    return new Bucket(bucketname, config, streamingURI, nodes);
  }

  public void subscribe(String bucketName, Reconfigurable rec) {}

  public void markForResubscribe(String bucketName, Reconfigurable rec) {}

  public void unsubscribe(String vbucketName, Reconfigurable rec) {}

  public void shutdown() {}

  public String getAnonymousAuthBucket() {
    return "";
  }

  public void finishResubscribe() {}

  public Reconfigurable getReconfigurable() {
    throw new UnsupportedOperationException("Not needed in the mock?");
  }

  public String getBucket() {
    throw new UnsupportedOperationException("Not needed in the mock?");
  }

  @Override
  public void updateBucket(String string, Bucket bucket) { }

  @Override
  public void updateBaseListFromConfig(List<URI> baseList) {
    baseListUpdated = true;
  }
}
