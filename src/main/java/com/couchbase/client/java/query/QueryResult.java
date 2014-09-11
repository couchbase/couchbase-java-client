package com.couchbase.client.java.query;

import com.couchbase.client.java.document.json.JsonObject;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public interface QueryResult {

    List<QueryRow> allRows();

    List<QueryRow> allRows(long timeout, TimeUnit timeUnit);

    Iterator<QueryRow> rows();

    Iterator<QueryRow> rows(long timeout, TimeUnit timeUnit);

    JsonObject info();

    JsonObject info(long timeout, TimeUnit timeUnit);

    boolean success();

    JsonObject error();

}
