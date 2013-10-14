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

import com.couchbase.client.protocol.views.AbstractView;
import com.couchbase.client.protocol.views.SpatialView;
import com.couchbase.client.protocol.views.SpatialViewRowWithDocs;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewResponseWithDocs;
import com.couchbase.client.protocol.views.ViewRow;
import com.couchbase.client.protocol.views.ViewRowWithDocs;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import net.spy.memcached.internal.BulkFuture;
import net.spy.memcached.internal.GenericCompletionListener;
import net.spy.memcached.ops.OperationStatus;

/**
 * A ViewFuture.
 */
public class ViewFuture extends HttpFuture<ViewResponse> {
  private final AtomicReference<BulkFuture<Map<String, Object>>> multigetRef;

  private final AbstractView view;

  public ViewFuture(CountDownLatch latch, long timeout, AbstractView view,
    ExecutorService service) {
    super(latch, timeout, service);
    this.multigetRef =
        new AtomicReference<BulkFuture<Map<String, Object>>>(null);
    this.view = view;
  }

  @Override
  public ViewResponse get(long duration, TimeUnit units)
    throws InterruptedException, ExecutionException, TimeoutException {
    waitForAndCheckOperation(duration, units);

    if (multigetRef.get() == null) {
      return null;
    }

    Map<String, Object> docMap = multigetRef.get().get();
    final ViewResponseWithDocs viewResp = (ViewResponseWithDocs) objRef.get();
    Collection<ViewRow> rows = new LinkedList<ViewRow>();
    Iterator<ViewRow> itr = viewResp.iterator();

    while (itr.hasNext()) {
      ViewRow r = itr.next();
      if(view instanceof SpatialView) {
        rows.add(new SpatialViewRowWithDocs(r.getId(), r.getBbox(),
          r.getGeometry(), r.getValue(), docMap.get(r.getId())));
      } else {
        rows.add(new ViewRowWithDocs(r.getId(), r.getKey(), r.getValue(),
          docMap.get(r.getId())));
      }
    }
    return new ViewResponseWithDocs(rows, viewResp.getErrors());
  }

  public void set(ViewResponse viewResponse,
      BulkFuture<Map<String, Object>> oper, OperationStatus s) {
    objRef.set(viewResponse);
    multigetRef.set(oper);
    status = s;
    notifyListeners();
  }

  @Override
  public ViewFuture addListener(HttpCompletionListener listener) {
    super.addToListeners((GenericCompletionListener) listener);
    return this;
  }

  @Override
  public ViewFuture removeListener(HttpCompletionListener listener) {
    super.removeFromListeners((GenericCompletionListener) listener);
    return this;
  }
}
