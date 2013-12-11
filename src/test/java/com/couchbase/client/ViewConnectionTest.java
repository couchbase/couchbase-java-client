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

import com.couchbase.client.protocol.views.HttpOperation;
import com.couchbase.client.vbucket.config.Bucket;
import com.couchbase.client.vbucket.config.DefaultConfig;
import org.apache.http.HttpHost;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the correct functionality of the {@link ViewConnection} class.
 */
public class ViewConnectionTest {

  private static final String DEFAULT_USER = "default";
  private static final String DEFAULT_PASS = "";
  private static final int PORT = 8092;

  private CouchbaseConnectionFactory factoryMock;
  private Bucket bucketMock;
  private DefaultConfig configMock;

  @Before
  public void setup() {
    factoryMock = mock(CouchbaseConnectionFactory.class);
    when(factoryMock.getViewWorkerSize()).thenReturn(1);
    when(factoryMock.getViewConnsPerNode()).thenReturn(1);

    bucketMock = mock(Bucket.class);
    configMock = mock(DefaultConfig.class);

    when(bucketMock.getConfig()).thenReturn(configMock);
    when(configMock.nodeHasActiveVBuckets(any(InetSocketAddress.class)))
      .thenReturn(true);
  }

  @Test
  public void shouldInitializeWithHosts() throws Exception {
    List<InetSocketAddress> initialNodes = Arrays.asList(
      new InetSocketAddress("10.0.0.1", PORT),
      new InetSocketAddress("10.0.0.2", PORT)
    );

    ViewConnection conn = new ViewConnection(factoryMock, initialNodes,
      DEFAULT_USER, DEFAULT_PASS);

    List<HttpHost> connected = conn.getConnectedHosts();
    assertEquals(2, connected.size());
    assertEquals("10.0.0.1:" + PORT, connected.get(0).toHostString());
    assertEquals("10.0.0.2:" + PORT, connected.get(1).toHostString());
  }

  @Test
  public void shouldAddHostsOnRebalance() throws Exception {
    List<InetSocketAddress> initialNodes = Collections.singletonList(
      new InetSocketAddress("10.0.0.1", PORT));

    ViewConnection conn = new ViewConnection(factoryMock, initialNodes,
      DEFAULT_USER, DEFAULT_PASS);


    List<URL> viewServers = Arrays.asList(
      new URL("http://10.0.0.1:" + PORT),
      new URL("http://10.0.0.2:" + PORT),
      new URL("http://10.0.0.3:" + PORT)
    );
    when(configMock.getCouchServers()).thenReturn(viewServers);

    assertEquals(1, conn.getConnectedHosts().size());
    conn.reconfigure(bucketMock);

    List<HttpHost> connected = conn.getConnectedHosts();
    assertEquals(3, conn.getConnectedHosts().size());
    assertEquals("10.0.0.1:" + PORT, connected.get(0).toHostString());
    assertEquals("10.0.0.2:" + PORT, connected.get(1).toHostString());
    assertEquals("10.0.0.3:" + PORT, connected.get(2).toHostString());
  }

  @Test
  public void shouldRemoveHostsOnRebalance() throws Exception {
    List<InetSocketAddress> initialNodes = Arrays.asList(
      new InetSocketAddress("10.0.0.1", PORT),
      new InetSocketAddress("10.0.0.2", PORT),
      new InetSocketAddress("10.0.0.3", PORT)
    );

    ViewConnection conn = new ViewConnection(factoryMock, initialNodes,
      DEFAULT_USER, DEFAULT_PASS);


    List<URL> viewServers = Collections.singletonList(
      new URL("http://10.0.0.1:" + PORT));
    when(configMock.getCouchServers()).thenReturn(viewServers);

    assertEquals(3, conn.getConnectedHosts().size());
    conn.reconfigure(bucketMock);

    List<HttpHost> connected = conn.getConnectedHosts();
    assertEquals(1, conn.getConnectedHosts().size());
    assertEquals("10.0.0.1:" + PORT, connected.get(0).toHostString());
  }

  @Test
  public void shouldAddAndRemoveNodeOnRebalance() throws Exception {
    List<InetSocketAddress> initialNodes = Arrays.asList(
      new InetSocketAddress("10.0.0.1", PORT),
      new InetSocketAddress("10.0.0.2", PORT)
    );

    ViewConnection conn = new ViewConnection(factoryMock, initialNodes,
      DEFAULT_USER, DEFAULT_PASS);

    List<URL> viewServers = Arrays.asList(
      new URL("http://10.0.0.3:" + PORT),
      new URL("http://10.0.0.4:" + PORT)
    );
    when(configMock.getCouchServers()).thenReturn(viewServers);

    assertEquals(2, conn.getConnectedHosts().size());
    conn.reconfigure(bucketMock);

    List<HttpHost> connected = conn.getConnectedHosts();
    assertEquals(2, conn.getConnectedHosts().size());
    assertEquals("10.0.0.3:" + PORT, connected.get(0).toHostString());
    assertEquals("10.0.0.4:" + PORT, connected.get(1).toHostString());
  }

  @Test
  public void shouldNotAddIfNoActiveVBucket() throws Exception {
    List<InetSocketAddress> initialNodes = Collections.singletonList(
      new InetSocketAddress("10.0.0.1", PORT));

    ViewConnection conn = new ViewConnection(factoryMock, initialNodes,
      DEFAULT_USER, DEFAULT_PASS);

    when(configMock.nodeHasActiveVBuckets(
      new InetSocketAddress("10.0.0.1", PORT))
    ).thenReturn(true);
    when(configMock.nodeHasActiveVBuckets(
      new InetSocketAddress("10.0.0.2", PORT))
    ).thenReturn(true);
    when(configMock.nodeHasActiveVBuckets(
      new InetSocketAddress("10.0.0.3", PORT))
    ).thenReturn(false);

    List<URL> viewServers = Arrays.asList(
      new URL("http://10.0.0.1:" + PORT),
      new URL("http://10.0.0.2:" + PORT),
      new URL("http://10.0.0.3:" + PORT)
    );
    when(configMock.getCouchServers()).thenReturn(viewServers);

    assertEquals(1, conn.getConnectedHosts().size());
    conn.reconfigure(bucketMock);

    List<HttpHost> connected = conn.getConnectedHosts();
    assertEquals(2, conn.getConnectedHosts().size());
    assertEquals("10.0.0.1:" + PORT, connected.get(0).toHostString());
    assertEquals("10.0.0.2:" + PORT, connected.get(1).toHostString());
  }

  @Test
  public void shouldRemoveIfNoActiveVBucket() throws Exception {
    List<InetSocketAddress> initialNodes = Arrays.asList(
      new InetSocketAddress("10.0.0.1", PORT),
      new InetSocketAddress("10.0.0.2", PORT)
    );

    ViewConnection conn = new ViewConnection(factoryMock, initialNodes,
      DEFAULT_USER, DEFAULT_PASS);

    when(configMock.nodeHasActiveVBuckets(
      new InetSocketAddress("10.0.0.1", PORT))
    ).thenReturn(true);
    when(configMock.nodeHasActiveVBuckets(
      new InetSocketAddress("10.0.0.2", PORT))
    ).thenReturn(false);

    List<URL> viewServers = Arrays.asList(
      new URL("http://10.0.0.1:" + PORT),
      new URL("http://10.0.0.2:" + PORT)
    );
    when(configMock.getCouchServers()).thenReturn(viewServers);

    assertEquals(2, conn.getConnectedHosts().size());
    conn.reconfigure(bucketMock);

    List<HttpHost> connected = conn.getConnectedHosts();
    assertEquals(1, conn.getConnectedHosts().size());
    assertEquals("10.0.0.1:" + PORT, connected.get(0).toHostString());
  }

  @Test
  public void shouldRoundRobinRequests() throws Exception {
    List<InetSocketAddress> initialNodes = Arrays.asList(
      new InetSocketAddress("10.0.0.1", PORT),
      new InetSocketAddress("10.0.0.2", PORT),
      new InetSocketAddress("10.0.0.3", PORT),
      new InetSocketAddress("10.0.0.4", PORT)
    );

    ViewConnection conn = new ViewConnection(factoryMock, initialNodes,
      DEFAULT_USER, DEFAULT_PASS);

    Map<HttpHost, Integer> hostCounts = new HashMap<HttpHost, Integer>();
    for (HttpHost host : conn.getConnectedHosts()) {
      hostCounts.put(host, 0);
    }

    for (int i = 0; i < 40; i++) {
      HttpHost host = conn.getNextNode();
      hostCounts.put(host, hostCounts.get(host) + 1);
    }

    for (HttpHost host : conn.getConnectedHosts()) {
      assertEquals(10, hostCounts.get(host).intValue());
    }
  }

  @Test
  public void shouldCancelOperationIfNoHostsInPlace() throws Exception {
    List<InetSocketAddress> initialNodes = Collections.emptyList();

    ViewConnection conn = new ViewConnection(factoryMock, initialNodes,
      DEFAULT_USER, DEFAULT_PASS);

    HttpOperation operationMock = mock(HttpOperation.class);
    conn.addOp(operationMock);
    verify(operationMock).cancel();
  }

  @Test
  public void shouldShutdownCleanly() throws Exception {
    List<InetSocketAddress> initialNodes = Arrays.asList(
      new InetSocketAddress("10.0.0.1", PORT),
      new InetSocketAddress("10.0.0.2", PORT),
      new InetSocketAddress("10.0.0.3", PORT),
      new InetSocketAddress("10.0.0.4", PORT)
    );

    ViewConnection conn = new ViewConnection(factoryMock, initialNodes,
      DEFAULT_USER, DEFAULT_PASS);

    assertTrue(conn.shutdown());
  }

}
