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
import com.couchbase.client.CouchbaseProperties;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import net.spy.memcached.BroadcastOpFactory;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StatsOperation;
import net.spy.memcached.protocol.binary.BinaryOperationFactory;

/**
 * The AdaptiveThrottler allows dynamic backoff of memcached operations to make
 * sure the server is not overloaded to more then a certain level.
 */
public class AdaptiveThrottler extends SpyObject implements Throttler {

  /**
   * Under normal conditions after how many operations the stats should be
   * re-checked.
   */
  private final int normalStatsInterval;

  /**
   * Under high watermark conditions (10% of the remaining high_wat) after
   * how many operations the stats should be re-checked.
   */
  private final int highStatsInterval;

  /**
   * Under critical high watermark conditions (higher than 10% of the high_wat
   * level) after how many operations the stats should be re-checked.
   */
  private final int criticalStatsInterval;

  /**
   * The amount of time (in ms) to sleep when high watermark conditions are
   * reached.
   */
  private final int highSleep;

  /**
   * The amount of time (in ms) to sleep when critical high watermark conditions
   * are reached.
   */
  private final int criticalSleep;

  /**
   * The current amount of operations on the counter (needed to check when
   * the stats need to be re-fetched).
   */
  private int intervalCounter = 0;

  /**
   * Holds a reference to the CouchbaseConnection in order to schedule stats
   * operations.
   */
  private CouchbaseConnection conn;

  private InetSocketAddress node;

  /**
   * Holds the current state of the throttler.
   */
  private ThrottlerState currentState = ThrottlerState.NORMAL;

  private BinaryOperationFactory opFact;

  /**
   * Initialize the Throttler with sensible default settings.
   *
   * Also, when the appropriate property settings are loaded (see the
   * CouchbaseProperties class for more information), those will be used
   * instead of the default settings.
   *
   * Note that there is a second constructor available that lets you set all
   * properties by hand. These are the default settings:
   *
   * - Normal Stats Interval Check: 10000 operations (normalStatsInterval)
   * - High Stats Interval Check: 100 operations (highStatsInterval)
   * - Critical Stats Interval Check: 10 operations (criticalStatsInterval)
   * - Time of throttle when High: 1ms (high_sleep_time)
   * - Time of throttle when critical: 3ms (critical_sleep_time)
   */
  public AdaptiveThrottler(CouchbaseConnection conn,
    BinaryOperationFactory opFact, InetSocketAddress node) {

    this(conn, opFact, node,
      Integer.parseInt(CouchbaseProperties.getProperty(
        "normal_stats_interval", "10000")),
      Integer.parseInt(CouchbaseProperties.getProperty(
        "high_stats_interval", "100")),
      Integer.parseInt(CouchbaseProperties.getProperty(
        "critical_stats_interval", "10")),
      Integer.parseInt(CouchbaseProperties.getProperty(
        "high_sleep_time", "1")),
      Integer.parseInt(CouchbaseProperties.getProperty(
        "critical_sleep_time", "3"))
    );
  }

  /**
   * Construct the AdaptiveThrottler with all possible options.
   *
   * @param conn the CouchbaseConnection to work against.
   * @param opFact the BinaryOperationFactory to work against.
   * @param node the node for the throttler.
   * @param normalStatsInterval After how many operations a check should be
   *        initialized when memory is below high_wat.
   * @param highStatsInterval After how many operations a check should be
   *        initialized when memory is higher than high_wat (< 10%)
   * @param criticalStatsInterval After how many operations a check should be
   *        initialized when memory is higher than high_wat (> 10%)
   * @param highSleep The time (in ms) to throttle when high is reached.
   * @param criticalSleep The time (in ms) to throttle when critical is reached.
   */
  public AdaptiveThrottler(CouchbaseConnection conn,
    BinaryOperationFactory opFact, InetSocketAddress node,
    int normalStatsInterval, int highStatsInterval,
    int criticalStatsInterval, int highSleep, int criticalSleep) {
    this.conn = conn;
    this.opFact = opFact;
    this.node = node;
    this.normalStatsInterval = normalStatsInterval;
    this.highStatsInterval = highStatsInterval;
    this.criticalStatsInterval = criticalStatsInterval;
    this.highSleep = highSleep;
    this.criticalSleep = criticalSleep;

    logCreation();
  }

  /**
   * Throttle if needed based on the given throttle constraints.
   */
  @Override
  public void throttle() {
    ++intervalCounter;
    if(statsNeedFetch()) {
      Map<String, String> stats = gatherStats();
      int throttleTime = throttleNeeded(stats);
      if(throttleTime > 0) {
        getLogger().debug("Throttling operation for " + throttleTime + "ms");
        try {
          Thread.sleep(throttleTime);
        } catch (InterruptedException ex) {
          getLogger().warn("Interrupted while Throttling!");
          return;
        }
      }
      intervalCounter = 0;
    }
  }

  /**
   * Checks if throttling is needed and returns the correct time to
   * throttle (or 0 if no throttling is needed).
   *
   * @param stats stats to analyze for this node.
   * @return the number of ms to sleep.
   */
  private int throttleNeeded(Map<String, String> stats) {
    long highWater;
    long memUsed;

    try {
      highWater = Long.parseLong(stats.get("ep_mem_high_wat"));
      memUsed = Long.parseLong(stats.get("mem_used"));
    } catch(NumberFormatException ex) {
      getLogger().warn("Received throttle stats invalid, skipping interval.");
      return 0;
    }

    if(memUsed >= (highWater + highWater/10)) {
      currentState = ThrottlerState.CRITICAL;
      return criticalSleep;
    } else if(memUsed >= highWater) {
      currentState = ThrottlerState.HIGH;
      return highSleep;
    } else {
      currentState = ThrottlerState.NORMAL;
      return 0;
    }
  }

  /**
   * Gather appropriate statistics from the cluster/node.
   */
  private Map<String, String> gatherStats() {
    final Map<InetSocketAddress, Map<String, String>> rv =
        new HashMap<InetSocketAddress, Map<String, String>>();

    CountDownLatch blatch = conn.broadcastOperation(new BroadcastOpFactory() {
      @Override
      public Operation newOp(final MemcachedNode n,
          final CountDownLatch latch) {
        final InetSocketAddress sa = (InetSocketAddress)n.getSocketAddress();
        rv.put(sa, new HashMap<String, String>());
        return opFact.stats(null, new StatsOperation.Callback() {
          @Override
          public void gotStat(String name, String val) {
            rv.get(sa).put(name, val);
          }

          @SuppressWarnings("synthetic-access")
          @Override
          public void receivedStatus(OperationStatus status) {
            if (!status.isSuccess()) {
              getLogger().warn("Unsuccessful stats fetch: " + status);
            }
          }

          @Override
          public void complete() {
            latch.countDown();
          }
        });
      }
    });
    try {
      blatch.await(1000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted waiting for stats", e);
    }
    return rv.get(node);
  }

  /**
   * Check if stats need to be fetched.
   *
   * @return true when stats need to be fetched.
   */
  private boolean statsNeedFetch() {
    if(currentState == ThrottlerState.NORMAL
      && intervalCounter >= normalStatsInterval) {
      return true;
    } else if(currentState == ThrottlerState.HIGH
      && intervalCounter >= highStatsInterval) {
      return true;
    } else if(currentState == ThrottlerState.CRITICAL
      && intervalCounter >= criticalStatsInterval) {
      return true;
    }
    return false;
  }

  private void logCreation() {
    getLogger().info("AdaptiveThrottler instantiated with options "
      + "normal_stats_interval: " + this.normalStatsInterval
      + " high_stats_interval: " + this.highStatsInterval
      + " critical_stats_interval: " + this.criticalStatsInterval
      + " high_sleep: " + this.highSleep
      + " critical_sleep: " + this.criticalSleep
      + " - for node " + this.node);
  }

}
