/*
 * Copyright (c) 2016 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    /**
     * ANSI join "on" clause.
     */
    LetPath on(Expression expression);
}
