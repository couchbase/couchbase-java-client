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

import com.couchbase.client.ViewNode.EventLogger;
import com.couchbase.client.ViewNode.MyHttpRequestExecutionHandler;
import com.couchbase.client.http.AsyncConnectionManager;
import com.couchbase.client.http.RequeueOpCallback;
import com.couchbase.client.protocol.views.HttpOperation;
import com.couchbase.client.vbucket.Reconfigurable;
import com.couchbase.client.vbucket.config.Bucket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionObserver;
import net.spy.memcached.compat.SpyObject;

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
 * The ViewConnection class creates and manages the various connections
 * to the ViewNodes.
 */
public class ViewConnection extends SpyObject implements
  Reconfigurable {
  private static final int NUM_CONNS = 1;

  private volatile boolean shutDown = false;
  protected volatile boolean reconfiguring = false;
  protected volatile boolean running = true;

  private final ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();
  private final Lock rlock = rwlock.readLock();
  private final Lock wlock = rwlock.writeLock();

  private final CouchbaseConnectionFactory connFactory;
  private final Collection<ConnectionObserver> connObservers =
      new ConcurrentLinkedQueue<ConnectionObserver>();
  private List<ViewNode> couchNodes;
  private int nextNode;

  /**
   * Kickstarts the initialization and delegates the connection creation.
   *
   * @param cf the factory which contains neeeded information.
   * @param addrs the list of addresses to connect to.
   * @param obs the connection observers.
   * @throws IOException
   */
  public ViewConnection(CouchbaseConnectionFactory cf,
      List<InetSocketAddress> addrs, Collection<ConnectionObserver> obs)
    throws IOException {
    connFactory = cf;
    connObservers.addAll(obs);
    couchNodes = createConnections(addrs);
    nextNode = 0;
  }

  /**
   * Create ViewNode connections and queue them up for connect.
   *
   * This method also defines the connection params for each connection,
   * including the default settings like timeouts and the user agent string.
   *
   * @param addrs addresses of all the nodes it should connect to.
   * @return Returns a list of the ViewNodes.
   * @throws IOException
   */
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
              protocolHandler, params, new RequeueOpCallback(this));
      getLogger().info("Added %s to connect queue", a.getHostName());

      ViewNode node = connFactory.createViewNode(a, connMgr);
      node.init();
      nodeList.add(node);
    }

    return nodeList;
  }

  /**
   * Write an operation to the next ViewNode.
   *
   * To make sure that the operations are distributed throughout the cluster,
   * the ViewNode is changed every time a new operation is added. Since the
   * getNextNode() method increments the ViewNode IDs and calculates the
   * modulo, the nodes are selected in a round-robin fashion.
   *
   * @param op the operation to run.
   */
  public void addOp(final HttpOperation op) {
    rlock.lock();
    try {
      if (couchNodes.isEmpty()) {
        getLogger().error("No server connections. Cancelling op.");
        op.cancel();
      } else {
        ViewNode node = couchNodes.get(getNextNode());
        node.writeOp(op);
      }
    } finally {
      rlock.unlock();
    }
  }

  /**
   * Calculates the next node to run the operation on.
   *
   * @return id of the next node.
   */
  private int getNextNode() {
    return nextNode = (++nextNode % couchNodes.size());
  }

  /**
   * Returns the currently connected ViewNodes.
   *
   * @return a list of currently connected ViewNodes.
   */
  public List<ViewNode> getConnectedNodes() {
    return couchNodes;
  }

  /**
   * Checks the state of the ViewConnection.
   *
   * If shutdown is currently in progress, an Exception is thrown.
   */
  protected void checkState() {
    if (shutDown) {
      throw new IllegalStateException("Shutting down");
    }
  }

  /**
   * Initiates the shutdown of all connected ViewNodes.
   *
   * @return false if a connection is already in progress, true otherwise.
   * @throws IOException
   */
  public boolean shutdown() throws IOException {
    if (shutDown) {
      getLogger().info("Suppressing duplicate attempt to shut down");
      return false;
    }

    shutDown = true;
    running = false;

    List<ViewNode> nodesToRemove = new ArrayList<ViewNode>();
    for(ViewNode node : couchNodes) {
      if (node != null) {
        String hostname = node.getSocketAddress().getHostName();
        if (node.hasWriteOps()) {
          getLogger().warn("Shutting down " + hostname +
            " with ops waiting to be written");
        } else {
          getLogger().info("Node " + hostname +
            " has no ops in the queue");
        }
        node.shutdown();
        nodesToRemove.add(node);
      }
    }

    for(ViewNode node : nodesToRemove) {
      couchNodes.remove(node);
    }

    return true;
  }

  /**
   * Reconfigures the connected ViewNodes.
   *
   * When a reconfiguration event happens, new ViewNodes may need to be added
   * or old ones need to be removed from the current configuration. This method
   * takes care that those operations are performed in the correct order and
   * are executed in a thread-safe manner.
   *
   * @param bucket the bucket which has been rebalanced.
   */
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
      ArrayList<ViewNode> shutdownNodes = new ArrayList<ViewNode>();
      ArrayList<ViewNode> stayNodes = new ArrayList<ViewNode>();
      ArrayList<InetSocketAddress> stayServers =
          new ArrayList<InetSocketAddress>();

      wlock.lock();
      try {
        for (ViewNode current : couchNodes) {
          if (newServerAddresses.contains(current.getSocketAddress())) {
            stayNodes.add(current);
            stayServers.add((InetSocketAddress) current.getSocketAddress());
          } else {
            shutdownNodes.add(current);
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

        couchNodes = mergedNodes;
      } finally {
        wlock.unlock();
      }

      // shutdown for the oddNodes
      for (ViewNode qa : shutdownNodes) {
        try {
          qa.shutdown();
        } catch (IOException e) {
          getLogger().error("Error shutting down connection to "
              + qa.getSocketAddress());
        }
      }

    } catch (IOException e) {
      getLogger().error("Connection reconfiguration failed", e);
    } finally {
      reconfiguring = false;
    }
  }
}
