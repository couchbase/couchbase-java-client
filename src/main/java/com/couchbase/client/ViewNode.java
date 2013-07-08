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

import com.couchbase.client.http.AsyncConnectionManager;
import com.couchbase.client.http.AsyncConnectionRequest;
import com.couchbase.client.http.HttpUtil;
import com.couchbase.client.http.RequestHandle;
import com.couchbase.client.protocol.views.HttpOperation;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import net.spy.memcached.compat.SpyObject;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.entity.BufferingNHttpEntity;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.EventListener;
import org.apache.http.nio.protocol.NHttpRequestExecutionHandler;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

/**
 * Establishes a HTTP connection to a single Couchbase node.
 *
 * Based upon http://hc.apache.org/httpcomponents-core-ga/httpcore-nio/
 * examples/org/apache/http/examples/nio/NHttpClientConnManagement.java
 */
public class ViewNode extends SpyObject {

  private final InetSocketAddress addr;
  private final AsyncConnectionManager connMgr;
  // TODO: These unused variables need to be utilized in the
  // AsyncConnectionManager.
  private final long opQueueMaxBlockTime;
  private final long opQueueLen;
  private final long defaultOpTimeout;
  private final String user;
  private final String pass;
  private boolean shuttingDown = false;

  private Thread ioThread;

  public ViewNode(InetSocketAddress a, AsyncConnectionManager mgr,
      long queueLen, long maxBlockTime, long operationTimeout, String usr,
      String pwd) {
    addr = a;
    connMgr = mgr;
    opQueueMaxBlockTime = maxBlockTime;
    opQueueLen = queueLen;
    defaultOpTimeout = operationTimeout;
    user = usr;
    pass = pwd;
  }

  public void init() throws IOReactorException {
    // Start the I/O reactor in a separate thread
    ioThread = new Thread(new Runnable() {
      public void run() {
        try {
          connMgr.execute();
        } catch (InterruptedIOException ex) {
          getLogger().error("I/O reactor Interrupted", ex);
        } catch (IOException e) {
          getLogger().error("I/O error: " + e.getMessage(), e);
        }
        getLogger().info("I/O reactor terminated for " + addr.getHostName());
      }
    }, "Couchbase View Thread for node " + addr);
    ioThread.start();
  }

  public boolean writeOp(HttpOperation op) {
    AsyncConnectionRequest connRequest = connMgr.requestConnection();
    try {
      connRequest.waitFor();
    } catch (InterruptedException e) {
      getLogger().warn(
          "Interrupted while trying to get a connection.");
      connRequest.cancel();
      return false;
    }

    NHttpClientConnection conn = connRequest.getConnection();
    if (conn == null) {
      getLogger().debug("Failed to obtain connection on node " + this.addr);
      connRequest.cancel();
      return false;
    } else {
      if (!user.equals("default")) {
        try {
          op.addAuthHeader(HttpUtil.buildAuthHeader(user, pass));
        } catch (UnsupportedEncodingException ex) {
          getLogger().error("Could not create auth header for request, "
            + "could not encode credentials into base64. Canceling op."
            + op, ex);
          op.cancel();
          connRequest.cancel();
        }
      }
      HttpContext context = conn.getContext();
      RequestHandle handle = new RequestHandle(connMgr, conn);
      context.setAttribute("request-handle", handle);
      context.setAttribute("operation", op);
      conn.requestOutput();
    }

    return true;
  }

  public boolean hasWriteOps() {
    return connMgr.hasPendingRequests();
  }

  public InetSocketAddress getSocketAddress() {
    return addr;
  }

  public void shutdown() throws IOException {
    shutdown(0, TimeUnit.MILLISECONDS);
  }

  public void shutdown(long time, TimeUnit unit) throws
    IOException {
    shuttingDown = true;
    long waittime = time;
    if (unit != TimeUnit.MILLISECONDS) {
      waittime = TimeUnit.MILLISECONDS.convert(time, unit);
    }

    connMgr.shutdown(waittime);
    try {
      ioThread.join(waittime);
    } catch (InterruptedException ex) {
      getLogger().error("Interrupt " + ex + " received while waiting for node "
        + addr.getHostName() + " to shut down.");
    }
  }

  public boolean isShuttingDown() {
    return shuttingDown;
  }

  static class MyHttpRequestExecutionHandler extends SpyObject
    implements NHttpRequestExecutionHandler  {

    private final ViewConnection vconn;

    public MyHttpRequestExecutionHandler(ViewConnection vconn) {
      super();
      this.vconn = vconn;
    }

    public void initalizeContext(final HttpContext context,
        final Object attachment) {
    }

    public void finalizeContext(final HttpContext context) {
      RequestHandle handle =
          (RequestHandle) context.removeAttribute("request-handle");
      if (handle != null) {
        handle.cancel();
      }
    }

    public HttpRequest submitRequest(final HttpContext context) {
      HttpOperation op = (HttpOperation) context.getAttribute("operation");
      if (op == null) {
        return null;
      }
      return op.getRequest();
    }

    public void handleResponse(final HttpResponse response,
        final HttpContext context) {
      RequestHandle handle =
          (RequestHandle) context.removeAttribute("request-handle");
      HttpOperation op = (HttpOperation) context.removeAttribute("operation");

      try {
        response.setEntity(new BufferedHttpEntity(response.getEntity()));
      } catch(IOException ex) {
        throw new RuntimeException("Could not convert HttpEntity content.");
      }

      int statusCode = response.getStatusLine().getStatusCode();
      if (handle != null) {
        boolean shouldRetry = shouldRetry(statusCode, response);
        if(shouldRetry) {
          if(!op.isTimedOut() && !op.isCancelled()) {
            getLogger().info("Retrying HTTP operation Request: "
              + op.getRequest().getRequestLine() + ", Response: "
              + response.getStatusLine());
            vconn.addOp(op);
          }
        } else {
          op.handleResponse(response);
        }
        handle.completed();
      }
    }

    private boolean shouldRetry(int statusCode, HttpResponse response) {
      switch(statusCode) {
      case 200:
        return false;
      case 404:
        return analyse404Response(response);
      case 500:
        return analyse500Response(response);
      case 300:
      case 301:
      case 302:
      case 303:
      case 307:
      case 401:
      case 408:
      case 409:
      case 412:
      case 416:
      case 417:
      case 501:
      case 502:
      case 503:
      case 504:
        return true;
      default:
        return false;
      }
    }

    private boolean analyse404Response(HttpResponse response) {
      try {
        String body = EntityUtils.toString(response.getEntity());
        // Indicates a Not Found Design Document
        if(body.contains("not_found")
          && (body.contains("missing") || body.contains("deleted"))) {
          return false;
        }
      } catch(IOException ex) {
        return false;
      }
      return true;
    }

    private boolean analyse500Response(HttpResponse response) {
      try {
        String body = EntityUtils.toString(response.getEntity());
        // Indicates a Not Found Design Document
        if(body.contains("error")
          && body.contains(("{not_found, missing_named_view}"))) {
          return false;
        }
      } catch(IOException ex) {
        return false;
      }
      return true;
    }

    @Override
    public ConsumingNHttpEntity responseEntity(HttpResponse response,
        HttpContext context) throws IOException {
      return new BufferingNHttpEntity(response.getEntity(),
          new HeapByteBufferAllocator());
    }
  }

  static class EventLogger extends SpyObject implements EventListener {

    public void connectionOpen(final NHttpConnection conn) {
      getLogger().debug("Connection open: " + conn);
    }

    public void connectionTimeout(final NHttpConnection conn) {
      getLogger().error("Connection timed out: " + conn);
    }

    public void connectionClosed(final NHttpConnection conn) {
      getLogger().debug("Connection closed: " + conn);
    }

    public void fatalIOException(final IOException ex,
        final NHttpConnection conn) {
      getLogger().error("I/O error: " + ex.getMessage());
    }

    public void fatalProtocolException(final HttpException ex,
        final NHttpConnection conn) {
      getLogger().error("HTTP error: " + ex.getMessage());
    }
  }
}
