/**
 * Copyright (C) 2009-2012 Couchbase, Inc.
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.TestConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Verifies the correct functionality of the ViewConnection class.
 */
public class ViewConnectionTest {

  /**
   * Tests the correctness of the initialization and shutdown phase.
   */
  @Test
  public void testInitAndShutdown() throws IOException, InterruptedException {

    CouchbaseConnectionFactory cf = new CouchbaseConnectionFactory(
      Arrays.asList(
        URI.create("http://" + TestConfig.IPV4_ADDR + ":8091/pools")
      ),
      "default",
      ""
    );

    List<InetSocketAddress> addrs = AddrUtil.getAddressesFromURL(
      cf.getVBucketConfig().getCouchServers()
    );

    ViewConnection vconn = cf.createViewConnection(addrs);
    assertFalse(vconn.getConnectedNodes().isEmpty());
    vconn.shutdown();
    assertTrue(vconn.getConnectedNodes().isEmpty());

  }

}
