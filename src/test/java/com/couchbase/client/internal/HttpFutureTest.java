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

import com.couchbase.client.protocol.views.HttpOperation;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Verifies the correct functionality of HttpFutures.
 */
public class HttpFutureTest {

  @Test
  public void testCancellation() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    long timeout = 1000;
    ExecutorService service = Executors.newCachedThreadPool();
    HttpFuture<CancellableOperation> future =
      new HttpFuture<CancellableOperation>(latch, timeout, service);
    HttpOperation op = new CancellableOperation();
    latch.countDown();
    future.setOperation(op);
    future.cancel(true);
    try {
      future.get();
      assertTrue("Future did not throw ExecutionException", false);
    } catch(ExecutionException e) {
      assertTrue(e.getCause() instanceof CancellationException);
      assertEquals("Cancelled", e.getCause().getMessage());
    } catch(Exception e) {
      assertTrue(e.getMessage(), false);
    }

  }

  static class CancellableOperation implements HttpOperation {

    private boolean cancelled = false;

    @Override
    public HttpRequest getRequest() {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public OperationCallback getCallback() {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isCancelled() {
      return cancelled;
    }

    @Override
    public boolean hasErrored() {
      return false;
    }

    @Override
    public boolean isTimedOut() {
      return false;
    }

    @Override
    public void cancel() {
      cancelled = true;
    }

    @Override
    public void timeOut() {

    }

    @Override
    public void addAuthHeader(String string) {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public OperationException getException() {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void handleResponse(HttpResponse hr) {
      throw new UnsupportedOperationException("Not supported yet.");
    }

  }
}
