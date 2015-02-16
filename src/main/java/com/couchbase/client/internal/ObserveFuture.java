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

import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.ops.OperationStatus;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

/**
 * A future that allows to chain operations with observe calls.
 */
public class ObserveFuture<T> extends OperationFuture<T> {

  private volatile boolean cancelled;
  private volatile boolean done;

  public ObserveFuture(final String k, final CountDownLatch l,
    final long opTimeout, final ExecutorService service) {
    super(k, l, opTimeout, service);

    cancelled = false;
    done = false;
  }

  @Override
  public boolean cancel() {
    cancelled = true;
    done = true;
    notifyListeners();
    return true;
  }

  @Override
  public boolean cancel(boolean ign) {
    return cancel();
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public boolean isDone() {
    return done;
  }

  @Override
  public void set(T o, OperationStatus s) {
    super.set(o, s);
    done = true;
  }
}
