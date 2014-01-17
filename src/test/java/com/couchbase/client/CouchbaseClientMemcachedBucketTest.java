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

package com.couchbase.client;

import com.couchbase.client.vbucket.provider.ConfigurationProvider;
import com.couchbase.client.vbucket.ConfigurationProviderMemcacheMock;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import net.spy.memcached.TestConfig;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests the correct initialization and reconfiguration of a memcached-bucket.
 */
public class CouchbaseClientMemcachedBucketTest {

  /**
   * Check the memcached bucket initialization.
   *
   * @pre Retrieve the server configuration stored in TestConfig.
   * Create a couchbase connection factory instance using the retrieved
   * configuration, memcached-bucket and the configuration provider instance.
   * Create a client with the connection factory, reconfigure the client
   * and shut it down.
   * @post Test succeeds and an assert is returned with the message
   * that the initialization was successful.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  public void testMemcacheBucketInitialization() throws IOException {
    boolean success = true;

    try {
      List<URI> baseList = new ArrayList<URI>();
      baseList.add(URI.create("http://"+TestConfig.IPV4_ADDR+":8091/pools"));
      ConfigurationProvider provider = new ConfigurationProviderMemcacheMock("memcached-default");

      CouchbaseConnectionFactoryMock factory;
      factory = new CouchbaseConnectionFactoryMock(
        baseList,
        "memcached-default",
        "",
        provider
      );

      CouchbaseClient client = new CouchbaseClient(factory);
      client.reconfigure(factory.getBucket("memcached-default"));
      client.shutdown();
    } catch(Exception e) {
      success = false;
    }

    assertTrue("Could not verify the init of a memcache bucket", success);
  }

}
