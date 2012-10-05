/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.client.protocol.views;

import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedList;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationErrorType;
import net.spy.memcached.ops.OperationException;
import net.spy.memcached.ops.OperationStatus;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author michael
 */
public class DesignDocOperationImpl extends HttpOperationImpl implements DesignDocOperation {

  public DesignDocOperationImpl(HttpRequest request, OperationCallback operationCallback) {
    super(request, operationCallback);
  }

  @Override
  public void handleResponse(HttpResponse response) {
    String json = getEntityString(response);

    int errorcode = response.getStatusLine().getStatusCode();
    try {
      OperationStatus status = parseViewForStatus(json, errorcode);
      callback.receivedStatus(status);
    } catch (ParseException e) {
      setException(new OperationException(OperationErrorType.GENERAL,
          "Error parsing JSON" + e));
    }
    callback.complete();
  }

}


