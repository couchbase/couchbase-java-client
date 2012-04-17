/**
 * Copyright (C) 2009-2011 Couchbase, Inc.
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

import com.couchbase.client.ViewNode.EventLogger;
import com.couchbase.client.ViewNode.MyHttpRequestExecutionHandler;
import com.couchbase.client.http.AsyncConnectionManager;
import com.couchbase.client.protocol.views.HttpOperation;
import com.couchbase.client.vbucket.Reconfigurable;
import com.couchbase.client.vbucket.config.Bucket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionObserver;
import net.spy.memcached.compat.SpyThread;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.nio.protocol.AsyncNHttpClientHandler;
import org.apache.http.nio.util.DirectByteBufferAllocator;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;


/**
 * Couchbase implementation of ViewConnection.
 *
 */
public class ViewConnection extends SpyThread  implements
  Reconfigurable {
  private static final int NUM_CONNS = 1;

  private volatile boolean shutDown = false;
  protected volatile boolean reconfiguring = false;
  protected volatile boolean running = true;

  private final CouchbaseConnectionFactory connFactory;
  private final ConcurrentLinkedQueue<ViewNode> nodesToShutdown;
  private final Collection<ConnectionObserver> connObservers =
      new ConcurrentLinkedQueue<ConnectionObserver>();
  private List<ViewNode> couchNodes;
  private int nextNode;

  public ViewConnection(CouchbaseConnectionFactory cf,
      List<InetSocketAddress> addrs, Collection<ConnectionObserver> obs)
    throws IOException {
    connFactory = cf;
    nodesToShutdown = new ConcurrentLinkedQueue<ViewNode>();
    connObservers.addAll(obs);
    couchNodes = createConnections(addrs);
    nextNode = 0;
    start();
  }

  private List<ViewNode> createConnections(List<InetSocketAddress> addrs)
    throws IOException {

    List<ViewNode> nodeList = new LinkedList<ViewNode>();

    for (InetSocketAddress a : addrs) {
      HttpParams params = new SyncBasicHttpParams();
      params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
          .setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000)
          .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
          .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK,
              false)
          .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
          .setParameter(CoreProtocolPNames.USER_AGENT,
              "Couchbase Java Client 1.0.2");

      HttpProcessor httpproc =
          new ImmutableHttpProcessor(new HttpRequestInterceptor[] {
            new RequestContent(), new RequestTargetHost(),
            new RequestConnControl(), new RequestUserAgent(),
            new RequestExpectContinue(), });

      AsyncNHttpClientHandler protocolHandler =
          new AsyncNHttpClientHandler(httpproc,
              new MyHttpRequestExecutionHandler(),
              new DefaultConnectionReuseStrategy(),
              new DirectByteBufferAllocator(), params);
      protocolHandler.setEventListener(new EventLogger());

      AsyncConnectionManager connMgr =
          new AsyncConnectionManager(
              new HttpHost(a.getHostName(), a.getPort()), NUM_CONNS,
              protocolHandler, params);
      getLogger().info("Added %s to connect queue", a);

      ViewNode node = connFactory.createViewNode(a, connMgr);
      node.init();
      nodeList.add(node);
    }

    return nodeList;
  }

  public void addOp(final HttpOperation op) {
    couchNodes.get(getNextNode()).addOp(op);
  }

  public void handleIO() {
    for (ViewNode node : couchNodes) {
      node.doWrites();
    }

    for (ViewNode qa : nodesToShutdown) {
      nodesToShutdown.remove(qa);
      Collection<HttpOperation> notCompletedOperations = qa.destroyWriteQueue();
      try {
        qa.shutdown();
      } catch (IOException e) {
        getLogger().error("Error shutting down connection to "
            + qa.getSocketAddress());
      }
      redistributeOperations(notCompletedOperations);
    }
  }

  private void redistributeOperations(Collection<HttpOperation> ops) {
    int added = 0;
    for (HttpOperation op : ops) {
      addOp(op);
      added++;
    }
    assert added > 0 : "Didn't add any new operations when redistributing";
  }

  private int getNextNode() {
    return nextNode = (++nextNode % couchNodes.size());
  }

  protected void checkState() {
    if (shutDown) {
      throw new IllegalStateException("Shutting down");
    }
    assert isAlive();
  }

  public boolean shutdown() throws IOException {
    if (shutDown) {
      getLogger().info("Suppressing duplicate attempt to shut down");
      return false;
    }
    shutDown = true;
    running = false;
    for (ViewNode n : couchNodes) {
      if (n != null) {
        n.shutdown();
        interrupt();
        if (n.hasWriteOps()) {
          getLogger().warn("Shutting down with ops waiting to be written");
        }
      }
    }
    return true;
  }

  public void reconfigure(Bucket bucket) {
    reconfiguring = true;
    try {
      // get a new collection of addresses from the received config
      HashSet<SocketAddress> newServerAddresses = new HashSet<SocketAddress>();
      List<InetSocketAddress> newServers =
        AddrUtil.getAddressesFromURL(bucket.getConfig().getCouchServers());
      for (InetSocketAddress server : newServers) {
        // add parsed address to our collections
        newServerAddresses.add(server);
      }

      // split current nodes to "odd nodes" and "stay nodes"
      ArrayList<ViewNode> oddNodes = new ArrayList<ViewNode>();
      ArrayList<ViewNode> stayNodes = new ArrayList<ViewNode>();
      ArrayList<InetSocketAddress> stayServers =
          new ArrayList<InetSocketAddress>();
      for (ViewNode current : couchNodes) {
        if (newServerAddresses.contains(current.getSocketAddress())) {
          stayNodes.add(current);
          stayServers.add((InetSocketAddress) current.getSocketAddress());
        } else {
          oddNodes.add(current);
        }
      }

      // prepare a collection of addresses for new nodes
      newServers.removeAll(stayServers);

      // create a collection of new nodes
      List<ViewNode> newNodes = createConnections(newServers);

      // merge stay nodes with new nodes
      List<ViewNode> mergedNodes = new ArrayList<ViewNode>();
      mergedNodes.addAll(stayNodes);
      mergedNodes.addAll(newNodes);

      // call update locator with new nodes list and vbucket config
      couchNodes = mergedNodes;

      // schedule shutdown for the oddNodes
      nodesToShutdown.addAll(oddNodes);
    } catch (IOException e) {
      getLogger().error("Connection reconfiguration failed", e);
    } finally {
      reconfiguring = false;
    }
  }

  /**
   * Infinitely loop processing IO.
   */
  @Override
  public void run() {
    while (running) {
      if (!reconfiguring) {
        try {
          handleIO();
        } catch (IllegalStateException e) {
          logRunException(e);
        } catch (Exception e) {
          logRunException(e);
        }
      }
    }
    getLogger().info("Shut down Couchbase client");
  }

  private void logRunException(Exception e) {
    if (shutDown) {
      // There are a couple types of errors that occur during the
      // shutdown sequence that are considered OK. Log at debug.
      getLogger().debug("Exception occurred during shutdown", e);
    } else {
      getLogger().warn("Problem handling Couchbase IO", e);
    }
  }
}
