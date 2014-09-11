package com.couchbase.client.java.query;

import com.couchbase.client.java.document.json.JsonObject;
import rx.Observable;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public interface AsyncQueryResult {

    Observable<AsyncQueryRow> rows();

    Observable<JsonObject> info();

    boolean success();

    JsonObject error();

}
