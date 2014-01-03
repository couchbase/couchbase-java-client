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

import java.net.URI;
import java.util.List;

/**
 * A builder to configure settings for the {@link ClusterManager}.
 *
 * If a configuration setting is not overridden, the default one will be
 * applied automatically.
 */
public class ClusterManagerBuilder {

  private int connectionTimeout = ClusterManager.DEFAULT_CONN_TIMEOUT;
  private int socketTimeout = ClusterManager.DEFAULT_SOCKET_TIMEOUT;
  private boolean tcpNoDelay = ClusterManager.DEFAULT_TCP_NODELAY;
  private int ioThreadCount = ClusterManager.DEFAULT_IO_THREADS;
  private int connectionsPerNode = ClusterManager.DEFAULT_CONNS_PER_NODE;

  /**
   * Returns the connection timeout.
   *
   * @return the connection timeout.
   */
  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  /**
   * Returns the socket timeout for a connection.
   *
   * @return the socket timeout.
   */
  public int getSocketTimeout() {
    return socketTimeout;
  }

  /**
   * Whether TCP NODELAY is used or not.
   *
   * @return true if nodelay is set, false otherwise.
   */
  public boolean getTcpNoDelay() {
    return tcpNoDelay;
  }

  /**
   * Returns the HTTP IO (worker) count.
   *
   * @return the worker count.
   */
  public int getIoThreadCount() {
    return ioThreadCount;
  }

  /**
   * Returns the max connections per node.
   *
   * @return the connections per node.
   */
  public int getConnectionsPerNode() {
    return connectionsPerNode;
  }

  /**
   * Set the HTTP connection timeout (2 minutes by default).
   *
   * @param connectionTimeout the timeout of the connection.
   */
  public ClusterManagerBuilder setConnectionTimeout(int connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
    return this;
  }

  /**
   * The HTTP connection socket timeout (2 minutes by default).
   *
   * @param socketTimeout the timeout of the socket on connect.
   */
  public ClusterManagerBuilder setSocketTimeout(int socketTimeout) {
    this.socketTimeout = socketTimeout;
    return this;
  }

  /**
   * The TCP NODELAY setting (true by default).
   *
   * @param tcpNoDelay the tcp nodelay setting.
   */
  public ClusterManagerBuilder setTcpNoDelay(boolean tcpNoDelay) {
    this.tcpNoDelay = tcpNoDelay;
    return this;
  }

  /**
   * The number of IO worker threads to use (1 by default).
   *
   * @param ioThreadCount io thread worker count.
   */
  public ClusterManagerBuilder setIoThreadCount(int ioThreadCount) {
    this.ioThreadCount = ioThreadCount;
    return this;
  }

  /**
   * The maximum number of parallel connections per node to open (5 per
   * default).
   *
   * @param connectionsPerNode number of connections.
   */
  public ClusterManagerBuilder setConnectionsPerNode(int connectionsPerNode) {
    this.connectionsPerNode = connectionsPerNode;
    return this;
  }

  /**
   * Builder a {@link ClusterManager}.
   *
   * @param nodes the list of nodes in the cluster to connect to.
   * @param username the admin username.
   * @param password the admin password.
   * @return the built {@link ClusterManager}.
   */
  public ClusterManager build(final List<URI> nodes, final String username,
    final String password) {
    return new ClusterManager(nodes, username, password, connectionTimeout,
      socketTimeout, tcpNoDelay, ioThreadCount, connectionsPerNode);
  }

}
