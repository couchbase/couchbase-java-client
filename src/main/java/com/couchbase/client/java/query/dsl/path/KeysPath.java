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

    /**
     * the on-key clause of a join/nest/unnest clause
     * with a single token key (eg. ON KEYS s.id).
     */
    LetPath onKeys(String key);

    /**
     * the on-key clause of a join/nest/unnest clause
     * with an array of constant keys (eg. ON KEYS ["a", "b"]).
     */
    LetPath onKeys(JsonArray keys);

    /**
     * the on-key clause of a join/nest/unnest clause
     * with 1-n constant keys (eg. ON KEYS "a" or ON KEYS ["a", "b"])
     */
    LetPath onKeysValues(String... constantKeys);

    /** use the primary keyspace (doc id) in a join clause) */
    LetPath useKeys(Expression expression);

    /**
     * use the primary keyspace (doc id) in a join clause), with
     * a single key given as a token expression (eg. USE KEYS s.id).
     */
    LetPath useKeys(String key);

    /**
     * use the primary keyspace (doc id) in a join clause, with
     * one or more keys given as constants (eg. USE KEYS "test" or
     * USE KEYS ["a", "b"])
     */
    LetPath useKeysValues(String... keys);

    /** use the primary keyspace (doc id) in a join clause) */
    LetPath useKeys(JsonArray keys);
}
