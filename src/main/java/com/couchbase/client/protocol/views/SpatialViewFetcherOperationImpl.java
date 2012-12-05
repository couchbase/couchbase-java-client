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

package com.couchbase.client.protocol.views;

import java.net.HttpURLConnection;
import java.text.ParseException;
import java.util.Iterator;

import net.spy.memcached.ops.OperationErrorType;
import net.spy.memcached.ops.OperationException;
import net.spy.memcached.ops.OperationStatus;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * A SpatialViewOperationImpl.
 */
public class SpatialViewFetcherOperationImpl extends HttpOperationImpl
  implements SpatialViewFetcherOperation {

  private final String bucketName;
  private final String designDocName;
  private final String viewName;

  public SpatialViewFetcherOperationImpl(HttpRequest r, String bucketName,
    String designDocName, String viewName, ViewFetcherCallback viewCallback) {
    super(r, viewCallback);
    this.bucketName = bucketName;
    this.designDocName = designDocName;
    this.viewName = viewName;
  }

  @Override
  public void handleResponse(HttpResponse response) {
    String json = getEntityString(response);
    try {
      SpatialView view = parseDesignDocumentForView(bucketName, designDocName,
          viewName, json);
      int errorcode = response.getStatusLine().getStatusCode();
      if (errorcode == HttpURLConnection.HTTP_OK) {
        ((SpatialViewFetcherOperation.ViewFetcherCallback) callback)
          .gotData(view);
        callback.receivedStatus(new OperationStatus(true, "OK"));
      } else {
        callback.receivedStatus(new OperationStatus(false,
            Integer.toString(errorcode)));
      }
    } catch (ParseException e) {
      exception = new OperationException(OperationErrorType.GENERAL,
        "Error parsing JSON");
    }
    callback.complete();
  }

  private SpatialView parseDesignDocumentForView(String dn, String ddn,
      String viewname, String json) throws ParseException {
    SpatialView view = null;
    if (json != null) {
      try {
        JSONObject base = new JSONObject(json);
        if (base.has("error")) {
          return null;
        }
        if (base.has("spatial")) {
          JSONObject views = base.getJSONObject("spatial");
          Iterator<?> itr = views.keys();
          while (itr.hasNext()) {
            String curView = (String) itr.next();
            if (curView.equals(viewname)) {
              view = new SpatialView(dn, ddn, viewname);
              break;
            }
          }
        }
      } catch (JSONException e) {
        throw new ParseException("Cannot read json: " + json, 0);
      }
    }
    return view;
  }
}
