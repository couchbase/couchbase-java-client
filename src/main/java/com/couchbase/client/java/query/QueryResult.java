package com.couchbase.client.java.query;

import com.couchbase.client.java.document.json.JsonObject;
import rx.Observable;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public interface QueryResult {

    Observable<QueryRow> rows();

    Observable<JsonObject> info();

    boolean success();

    JsonObject error();

}
