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

    boolean parseSuccess();

    Observable<Boolean> finalSuccess();

    Observable<JsonObject> errors();

}
