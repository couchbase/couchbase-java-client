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

package com.couchbase.client.http;

import org.apache.http.HttpHost;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.nio.pool.BasicNIOConnPool;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.pool.PoolEntry;
import org.apache.http.pool.PoolEntryCallback;

import java.util.concurrent.TimeUnit;

/**
 * Extended Pool that allows for explicit removal of {@link HttpHost}s.
 */
public class ViewPool extends BasicNIOConnPool {

  /**
   * Create a new {@link ViewPool}.
   *
   * @param reactor the reactor to use for connections.
   * @param config the configuration to apply.
   */
  public ViewPool(final ConnectingIOReactor reactor,
    final ConnectionConfig config) {
    super(reactor, config);
  }

  /**
   * Closes the underlying connections for the given {@link HttpHost}.
   *
   * Since on a rebalance out, all connections to this view node need to
   * be closed, regardless if they are currently available or leased.
   *
   * @param host the host to shutdown connections for.
   */
  public void closeConnectionsForHost(final HttpHost host) {
    // In-flight operations will be cancelled and retried in other parts of
    // the codebase.
    enumAvailable(new PoolEntryCallback<HttpHost, NHttpClientConnection>() {
      @Override
      public void process(PoolEntry<HttpHost, NHttpClientConnection> entry) {
        if (entry.getRoute().equals(host)) {
          entry.updateExpiry(0, TimeUnit.MILLISECONDS);
        }
      }
    });

    enumLeased(new PoolEntryCallback<HttpHost, NHttpClientConnection>() {
      @Override
      public void process(PoolEntry<HttpHost, NHttpClientConnection> entry) {
        if (entry.getRoute().equals(host)) {
          entry.updateExpiry(0, TimeUnit.MILLISECONDS);
        }
      }
    });

    closeExpired();
  }

}
