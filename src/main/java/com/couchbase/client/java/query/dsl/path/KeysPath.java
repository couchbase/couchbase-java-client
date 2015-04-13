package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.query.dsl.Expression;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public interface KeysPath extends LetPath {
    /** the on-key clause of a join/nest/unnest clause */
    LetPath onKeys(Expression expression);

    /** the on-key clause of a join/nest/unnest clause */
    LetPath onKeys(String... keys);

    /** the on-key clause of a join/nest/unnest clause */
    LetPath onKeys(JsonArray keys);

    /** use the primary keyspace (doc id) in a join clause) */
    LetPath useKeys(Expression expression);

    /** use the primary keyspace (doc id) in a join clause) */
    LetPath useKeys(String... keys);

    /** use the primary keyspace (doc id) in a join clause) */
    LetPath useKeys(JsonArray keys);
}
