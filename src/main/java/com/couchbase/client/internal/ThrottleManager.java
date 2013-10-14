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

package com.couchbase.client.internal;

import com.couchbase.client.CouchbaseConnection;
import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.spy.memcached.OperationFactory;

/**
 * The ThrottleManager handles Throttle instances which are bound to their
 * corresponding MemcachedNodes. It also handles automatic rebalance cleanup
 * and therefore acts as the managing frontend of the Throttle instances
 * which handle the actual throttling.
 */
public class ThrottleManager<T extends Throttler> {

  private static final Logger LOGGER = Logger.getLogger(
    ThrottleManager.class.getName());
  private final Map<InetSocketAddress, T> throttles;
  private final Class<T> throttler;
  private final CouchbaseConnection conn;
  private final OperationFactory opFact;

  public ThrottleManager(List<InetSocketAddress> initialNodes,
    Class<T> throttler, CouchbaseConnection conn, OperationFactory opFact) {
    this.throttler = throttler;
    this.throttles = new HashMap<InetSocketAddress, T>();
    this.conn = conn;
    this.opFact = opFact;

    for(InetSocketAddress node : initialNodes) {
      setThrottler(node);
    }
  }

  public final ThrottleManager setThrottler(InetSocketAddress node) {
    LOGGER.log(Level.INFO, "Adding Throttler for {0}", node.toString());

    try {
      Constructor<T> constructor = throttler.getConstructor(
        this.conn.getClass(), opFact.getClass(), node.getClass());
      throttles.put(node, constructor.newInstance(this.conn, opFact, node));
    } catch(Exception e) {
      throw new RuntimeException("Could not add Throttler for "
        + node.toString());
    }
    return this;
  }

  public T getThrottler(InetSocketAddress node) {
    return throttles.get(node);
  }

  public void removeThrottler(InetSocketAddress node) {
    LOGGER.log(Level.INFO, "Removing Throttler for {0}", node.toString());
    throttles.remove(node);
  }

}
