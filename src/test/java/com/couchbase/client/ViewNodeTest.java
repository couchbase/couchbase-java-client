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

import com.couchbase.client.http.AsyncConnectionManager;
import com.couchbase.client.http.RequeueOpCallback;
import com.couchbase.client.internal.HttpFuture;
import com.couchbase.client.protocol.views.HttpOperation;
import com.couchbase.client.protocol.views.NoDocsOperationImpl;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewOperation;
import com.couchbase.client.protocol.views.ViewResponse;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import net.spy.memcached.TestConfig;
import net.spy.memcached.ops.OperationStatus;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.protocol.AsyncNHttpClientHandler;
import org.apache.http.nio.reactor.IOReactorException;
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
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import static org.mockito.Mockito.mock;

/**
 * Tests the ViewNodes for their correct behavior.
 */
public class ViewNodeTest {

  @Test
  public void testUnresponsiveViewNode() throws IOReactorException {
    AsyncConnectionManager mgr = createConMgr(TestConfig.IPV4_ADDR, 8091);
    InetSocketAddress addr = new InetSocketAddress(TestConfig.IPV4_ADDR, 8091);
    ViewNode viewNode = new ViewNode(addr, mgr, 0, 0, 0, "", "");

    assertFalse("View node has write ops.", viewNode.hasWriteOps());

    HttpOperation operation = createHttpOperation();
    viewNode.writeOp(operation);
  }


  private HttpOperation createHttpOperation() {
    View view = new View("a", "b", "c", true, true);
    final CountDownLatch couchLatch = new CountDownLatch(1);
    final HttpFuture<ViewResponse> crv =
        new HttpFuture<ViewResponse>(couchLatch, 60000);
    final HttpRequest request = new BasicHttpRequest("GET", "/pools",
      HttpVersion.HTTP_1_1);
    return new NoDocsOperationImpl(
      request,
      view,
      new ViewOperation.ViewCallback() {
        private ViewResponse vr = null;

        @Override
        public void receivedStatus(OperationStatus status) {
          crv.set(vr, status);
        }

        @Override
        public void complete() {
          couchLatch.countDown();
        }

        @Override
        public void gotData(ViewResponse response) {
          vr = response;
        }
      }
    );
  }


  private AsyncConnectionManager createConMgr(String host, int port)
    throws IOReactorException {
    HttpHost target = new HttpHost(host, port);
    int maxConnections = 1;

    HttpParams params = new SyncBasicHttpParams();
    params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
          .setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000)
          .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
          .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK,
            false)
          .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
          .setParameter(CoreProtocolPNames.USER_AGENT,
            "Couchbase Java Client 1.1");

    HttpProcessor httpproc = new ImmutableHttpProcessor(
      new HttpRequestInterceptor[] {
        new RequestContent(),
        new RequestTargetHost(),
        new RequestConnControl(),
        new RequestUserAgent(),
        new RequestExpectContinue(),
      }
    );


    AsyncNHttpClientHandler protocolHandler = new AsyncNHttpClientHandler(
      httpproc,
      new ViewNode.MyHttpRequestExecutionHandler(),
      new DefaultConnectionReuseStrategy(),
      new DirectByteBufferAllocator(), params
    );

    protocolHandler.setEventListener(new ViewNode.EventLogger());
    RequeueOpCallback callback = mock(RequeueOpCallback.class);
    AsyncConnectionManager manager = new AsyncConnectionManager(
      target,
      maxConnections,
      protocolHandler,
      params,
      callback
    );

    return manager;
  }

}
