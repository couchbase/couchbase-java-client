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

import com.couchbase.client.protocol.views.HttpOperationImpl;
import java.io.IOException;
import java.io.InputStream;

import java.net.HttpURLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.spy.memcached.ops.OperationStatus;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

/**
 * A TestOperationPutImpl.
 */
public class TestOperationPutImpl extends HttpOperationImpl implements
    TestOperation {

  public TestOperationPutImpl(HttpRequest r, TestCallback testCallback) {
    super(r, testCallback);
  }

  @Override
  public void handleResponse(HttpResponse response) {
    StringBuilder json = new StringBuilder("");  // workaround for null returns
    json.append(getEntityString(response));
    int errorcode = response.getStatusLine().getStatusCode();
    // read the response into a string
    InputStream bi;
    StringBuffer responseContent = new StringBuffer("");
    try {
      bi = response.getEntity().getContent();
      byte[] buffer = new byte[bi.available() ];
      int bytesRead = bi.read(buffer);
      responseContent.append(new String(buffer));
    } catch (IOException ex) {
      Logger.getLogger(TestOperationImpl.class.getName()).log(
              Level.SEVERE, "Could not read test response.", ex);
    } catch (IllegalStateException ex) {
      Logger.getLogger(TestOperationImpl.class.getName()).log(
              Level.SEVERE, null, ex);
    }



    if (errorcode == HttpURLConnection.HTTP_CREATED) {
      ((TestCallback) callback).getData(json.toString());
      callback.receivedStatus(new OperationStatus(true, "OK"));
    } else {
      callback.receivedStatus(new OperationStatus(false,
          Integer.toString(errorcode) + ": " + responseContent));
    }
    callback.complete();
  }

}
