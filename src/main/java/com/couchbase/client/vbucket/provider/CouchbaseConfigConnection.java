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

package com.couchbase.client.vbucket.provider;

import com.couchbase.client.CouchbaseConnection;
import com.couchbase.client.CouchbaseConnectionFactory;
import com.couchbase.client.CouchbaseProperties;
import net.spy.memcached.BroadcastOpFactory;
import net.spy.memcached.ConnectionObserver;
import net.spy.memcached.FailureMode;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.OperationFactory;
import net.spy.memcached.compat.log.Level;
import net.spy.memcached.compat.log.Logger;
import net.spy.memcached.compat.log.LoggerFactory;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A custom {@link CouchbaseConnection} that is used for binary config
 * fetching.
 *
 * It is used to customize different codepaths and also transparently redirect
 * logging so that it makes sense as part of the overall application (without
 * having to modify a lot of code from the inherited classes).
 */
public class CouchbaseConfigConnection extends CouchbaseConnection {

  /**
   * If 3 consecutive NOOPS are missed, an outdated config is signaled.
   */
  private static final String DEFAULT_MISSED_NOOPS_THRESHOLD = "3";
  private final short noopsThreshold;
  private volatile short outstandingNoops = 0;

  private static final Logger LOGGER = new LoggerProxy(
      LoggerFactory.getLogger(CouchbaseConfigConnection.class)
  );

  public CouchbaseConfigConnection(int bufSize, CouchbaseConnectionFactory f,
    List<InetSocketAddress> a, Collection<ConnectionObserver> obs,
    FailureMode fm, OperationFactory opfactory) throws IOException {
    super(bufSize, f, a, obs, fm, opfactory);

    noopsThreshold = Short.decode(CouchbaseProperties.getProperty(
      "configPollThreshold",
      DEFAULT_MISSED_NOOPS_THRESHOLD
    ));

    LOGGER.debug("Using config noop threshold of " + noopsThreshold);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{CouchbaseConfigConnection to");
    for (MemcachedNode qa : locator.getAll()) {
      sb.append(" ").append(qa.getSocketAddress());
    }
    sb.append("}");
    return sb.toString();
  }

  @Override
  protected void handleWokenUpSelector() {
    long now = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());
    long diff = now - lastWrite;
    if (lastWrite > 0 && diff >= ALLOWED_IDLE_TIME) {
      if (outstandingNoops >= noopsThreshold) {
        cf.getConfigurationProvider().signalOutdated();
        outstandingNoops = 0;
      }

      updateLastWrite();
      getLogger().debug("Wakeup counter triggered, broadcasting noops.");
      final OperationFactory fact = cf.getOperationFactory();
      outstandingNoops++;
      broadcastOperation(new BroadcastOpFactory() {
        @Override
        public Operation newOp(MemcachedNode n, final CountDownLatch latch) {
          return fact.noop(new OperationCallback() {
            @Override
            public void receivedStatus(OperationStatus status) {
              if (status.isSuccess() && outstandingNoops > 0) {
                outstandingNoops--;
              }
            }

            @Override
            public void complete() {}
          });
        }
      });
    }
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  public static class LoggerProxy implements Logger {

    private final Logger logger;

    public LoggerProxy(Logger logger) {
      this.logger = logger;
    }

    @Override
    public String getName() {
      return logger.getName();
    }

    @Override
    public boolean isDebugEnabled() {
      return logger.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
      return logger.isInfoEnabled();
    }

    @Override
    public boolean isTraceEnabled() {
      return logger.isTraceEnabled();
    }

    @Override
    public void log(Level level, Object o, Throwable throwable) {
      logger.log(Level.DEBUG, o, throwable);
    }

    @Override
    public void log(Level level, Object o) {
      logger.log(Level.DEBUG, o);
    }

    @Override
    public void trace(Object o, Throwable throwable) {
      logger.trace(o, throwable);
    }

    @Override
    public void trace(Object o) {
      logger.trace(o);
    }

    @Override
    public void trace(String s, Object... objects) {
      logger.trace(s, objects);
    }

    @Override
    public void debug(Object o, Throwable throwable) {
      logger.debug(o, throwable);
    }

    @Override
    public void debug(Object o) {
      logger.debug(o);
    }

    @Override
    public void debug(String s, Object... objects) {
      logger.debug(s, objects);
    }

    @Override
    public void info(Object o, Throwable throwable) {
      logger.debug(o, throwable);
    }

    @Override
    public void info(Object o) {
      logger.debug(o);
    }

    @Override
    public void info(String s, Object... objects) {
      logger.debug(s, objects);
    }

    @Override
    public void warn(Object o, Throwable throwable) {
      logger.debug(o, throwable);
    }

    @Override
    public void warn(Object o) {
      logger.debug(o);
    }

    @Override
    public void warn(String s, Object... objects) {
      logger.debug(s, objects);
    }

    @Override
    public void error(Object o, Throwable throwable) {
      logger.debug(o, throwable);
    }

    @Override
    public void error(Object o) {
      logger.debug(o);
    }

    @Override
    public void error(String s, Object... objects) {
      logger.debug(s, objects);
    }

    @Override
    public void fatal(Object o, Throwable throwable) {
      logger.debug(o, throwable);
    }

    @Override
    public void fatal(Object o) {
      logger.debug(o);
    }

    @Override
    public void fatal(String s, Object... objects) {
      logger.debug(s, objects);
    }
  }

}
