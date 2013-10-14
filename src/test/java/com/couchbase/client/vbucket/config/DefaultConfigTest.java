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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.spy.memcached.DefaultHashAlgorithm;
import org.junit.Test;

import net.spy.memcached.HashAlgorithm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the correct output for a initialized {@link DefaultConfig}.
 */
public class DefaultConfigTest {

  private final HashAlgorithm hashAlgorithm = DefaultHashAlgorithm.CRC_HASH;

  /**
   * This test creates a mock VBucket configuration, where only 2 of the
   * three nodes given have VBuckets assigned to them.
   *
   * @throws Exception
   */
  @Test
  public void computesCachedServersWithVBuckets() throws Exception {
    List<String> servers = Arrays.asList("node1", "node2", "node3");
    List<URL> couchServers = Arrays.asList(new URL("http://node1:8092/"),
      new URL("http://node2:8092/"), new URL("http://node3:8092/"));

    final int numVBuckets = 32;
    List<VBucket> vbuckets = new ArrayList<VBucket>();
    for (int i = 0; i < numVBuckets; i++) {
      vbuckets.add(new VBucket(i % 2, new int[] {}));
    }

    DefaultConfig config = new DefaultConfig(
      hashAlgorithm, 3, 0, numVBuckets, servers, vbuckets, couchServers);
    assertTrue(config.nodeHasActiveVBuckets(new InetSocketAddress("node1", 8092)));
    assertTrue(config.nodeHasActiveVBuckets(new InetSocketAddress("node2", 8092)));
    assertFalse(config.nodeHasActiveVBuckets(new InetSocketAddress("node3", 8092)));
  }

}
