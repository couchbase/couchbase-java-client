package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.query.dsl.Expression;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public interface KeysPath extends LetPath {

    LetPath keys(Expression expression);

    LetPath keys(String key);

    LetPath keys(JsonArray keys);
}
