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

import com.couchbase.client.CouchbaseConnectionFactory;
import com.couchbase.client.ViewConnection;
import com.couchbase.client.protocol.views.HttpOperation;
import net.spy.memcached.compat.SpyObject;

import net.spy.memcached.compat.log.Logger;
import net.spy.memcached.compat.log.LoggerFactory;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

/**
 * Describes a {@link FutureCallback} for asynchronous View responses.
 *
 * This callback is called by the asynchronous executor for every response that
 * got returned from the view node. The code either dispatches parsing of the
 * actual content or decides to retry or abort, depending on the response status
 * code and values.
 */
public class HttpResponseCallback implements FutureCallback<HttpResponse> {

  /**
   * Static logger so that logging is also enabled for static methods.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(
    HttpResponseCallback.class);

  /**
   * The underlying HTTP request made.
   */
  private final HttpOperation op;

  /**
   * The dispatching {@link ViewConnection}.
   */
  private final ViewConnection vconn;

  /**
   * The host from where the response came.
   */
  private final HttpHost host;

  /**
   * Create a new callback.
   *
   * @param op the underlying operation.
   * @param vconn the view connection to reference.
   * @param host the target host from the response.
   */
  public HttpResponseCallback(final HttpOperation op,final ViewConnection vconn,
    final HttpHost host) {
    this.op = op;
    this.vconn = vconn;
    this.host = host;
  }

  @Override
  public void completed(final HttpResponse response) {
    try {
      response.setEntity(new BufferedHttpEntity(response.getEntity()));
    } catch(IOException ex) {
      throw new RuntimeException("Could not convert HttpEntity content.");
    }

    int statusCode = response.getStatusLine().getStatusCode();
    boolean shouldRetry = shouldRetry(statusCode, response);
    if (shouldRetry) {
      LOGGER.debug("Operation returned, but needs to be retried because "
        + "of: " + response.getStatusLine());
      retryOperation(op);
    } else {
      op.handleResponse(response);
    }
  }

  /**
   * Requeue the operation if it is not timed out or cancelled already.
   *
   * @param op the operation to retry.
   */
  private void retryOperation(final HttpOperation op) {
    if(!op.isTimedOut() && !op.isCancelled()) {
      LOGGER.debug("Retrying HTTP operation from node ("
        + host.toHostString() + "), Request: "
        + op.getRequest().getRequestLine());
      vconn.addOp(op);
    }
  }

  @Override
  public void failed(final Exception e) {
    if (e instanceof SocketTimeoutException) {
      retryOperation(op);
    } else if(e instanceof ConnectionClosedException
      || e instanceof ConnectException) {
      retryOperation(op);
      vconn.signalOutdatedConfig();
    } else {
      LOGGER.info("View Operation " + op.getRequest().getRequestLine()
        + " failed because of: ", e);
      op.cancel();
    }
  }

  @Override
  public void cancelled() {
    LOGGER.info("View Operation " + op.getRequest().getRequestLine()
      + " got cancelled.");
    op.cancel();
  }

  /**
   * Determine, based on the response code, if the operation should be retried.
   *
   * @param statusCode the code of the http response.
   * @param response the response content.
   *
   * @return true if retry, false if not.
   */
  private static boolean shouldRetry(final int statusCode,
    final HttpResponse response) {
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

  /**
   * Run deeper inspection on a 404 response, depending on the payload body.
   *
   * @param response the response to analyse.
   * @return true if retry, false if not.
   */
  private static boolean analyse404Response(HttpResponse response) {
    try {
      String body = EntityUtils.toString(response.getEntity());
      // Indicates a Not Found Design Document
      if(body.contains("not_found")
        && (body.contains("missing") || body.contains("deleted"))) {
        LOGGER.debug("Design Document not found, body: " + body);
        return false;
      }
    } catch(IOException ex) {
      return false;
    }
    return true;
  }

  /**
   * Run deeper inspection on a 500 response, depending on the payload body.
   *
   * @param response the response to analyse.
   * @return true if retry, false if not.
   */
  private static boolean analyse500Response(HttpResponse response) {
    try {
      String body = EntityUtils.toString(response.getEntity());
      // Indicates a Not Found Design Document
      if(body.contains("error")
        && body.contains("{not_found, missing_named_view}")) {
        LOGGER.debug("Design Document not found, body: " + body);
        return false;
      }
    } catch(IOException ex) {
      return false;
    }
    return true;
  }

}
