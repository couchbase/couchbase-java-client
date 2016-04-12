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
package com.couchbase.client.java.query;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;

import java.io.Serializable;

/**
 * Contract to describe N1QL queries. Queries are formed of a mandatory {@link Statement}
 * and optionally can have other components, as described in each implementation of this.
 *
 * Also exposes factory methods for different kinds of queries.
 *
 * @author Simon Baslé
 * @since 2.1
 */
public abstract class N1qlQuery implements Serializable {

    private static final long serialVersionUID = 3758119606237959729L;

    /**
     * Returns the {@link Statement} from this query. Note that this is the only mandatory
     * part of a N1QL query.
     *
     * @return the statement that forms the base of this query
     */
    public abstract Statement statement();

    /**
     * Returns the {@link N1qlParams} representing customization of the N1QL query.
     *
     * Note that this is different from named or positional parameters (which relate to the statement).
     *
     * @return the {@link N1qlParams} for this query, null if none.
     */
    public abstract N1qlParams params();

    /**
     * Convert this query to a full N1QL query in Json form.
     *
     * @return the json representation of this query (including all relevant parameters)
     */
    public abstract JsonObject n1ql();

    //== PRIVATE CLASS FOR RAW STATEMENT ==

    /* package */ static class RawStatement implements SerializableStatement {

        private static final long serialVersionUID = 107907431113912054L;

        private final String rawStatement;

        public RawStatement(String rawStatement) {
            this.rawStatement = rawStatement;
        }

        @Override
        public String toString() {
            return rawStatement;
        }
    }

    //========== FACTORY METHODS ==========
    /**
     * Create a new {@link N1qlQuery} with a plain un-parameterized {@link Statement}.
     *
     * @param statement the {@link Statement} to execute
     */
    public static SimpleN1qlQuery simple(Statement statement) {
        return new SimpleN1qlQuery(statement, null);
    }

    /**
     * Create a new {@link N1qlQuery} with a plain raw statement in String form.
     *
     * @param statement the raw statement string to execute (eg. "SELECT * FROM default").
     */
    public static SimpleN1qlQuery simple(String statement) {
        return simple(new RawStatement(statement));
    }

    /**
     * Create a new {@link N1qlQuery} with a plain un-parameterized {@link Statement} and
     * custom query parameters.
     *
     * @param statement the {@link Statement} to execute
     * @param params the {@link N1qlParams query parameters}.
     */
    public static SimpleN1qlQuery simple(Statement statement, N1qlParams params) {
        return new SimpleN1qlQuery(statement, params);
    }

    /**
     * Create a new {@link N1qlQuery} with a plain raw statement in {@link String} form and
     * custom query parameters.
     *
     * @param statement the raw statement string to execute (eg. "SELECT * FROM default").
     * @param params the {@link N1qlParams query parameters}.
     */
    public static SimpleN1qlQuery simple(String statement, N1qlParams params) {
        return simple(new RawStatement(statement), params);
    }

    //== PARAMETERIZED with Statement ==
    /**
     * Create a new query with positional parameters. Note that the {@link JsonArray}
     * should not be mutated until {@link #n1ql()} is called since it backs the
     * creation of the query string.
     *
     * Positional parameters have the form of `$n`, where the `n` represents the position, starting
     * with 1. The following two examples are equivalent and compare the {@link #simple(Statement)}
     * vs the positional {@link #parameterized(Statement, JsonArray)} approach:
     *
     * Simple:
     *
     * ```
     * N1qlQuery.simple("SELECT * FROM `travel-sample` WHERE type = 'airline' and name like 'A%'")
     * ```
     *
     * Positional Params:
     *
     * ```
     * N1qlQuery.parameterized(
     *  "SELECT * FROM `travel-sample` WHERE type = $1 and name like $2",
     *  JsonArray.from("airline", "A%")
     * )
     * ```
     *
     * Using parameterized statements combined with non-adhoc queries (which is configurable through
     * the {@link N1qlParams}) can provide better performance even when the actual arguments change
     * at execution time.
     *
     * @param statement the {@link Statement} to execute (containing positional placeholders)
     * @param positionalParams the values for the positional placeholders in statement
     */
    public static ParameterizedN1qlQuery parameterized(Statement statement, JsonArray positionalParams) {
        return new ParameterizedN1qlQuery(statement, positionalParams, null);
    }

    /**
     * Create a new query with named parameters. Note that the {@link JsonObject}
     * should not be mutated until {@link #n1ql()} is called since it backs the
     * creation of the query string.
     *
     * Named parameters have the form of `$name`, where the `name` represents the unique name. The
     * following two examples are equivalent and compare the {@link #simple(Statement)}
     * vs the named {@link #parameterized(Statement, JsonObject)} approach:
     *
     * Simple:
     *
     * ```
     * N1qlQuery.simple("SELECT * FROM `travel-sample` WHERE type = 'airline' and name like 'A%'")
     * ```
     *
     * Named Params:
     *
     * ```
     * N1qlQuery.parameterized(
     *  "SELECT * FROM `travel-sample` WHERE type = $type and name like $name",
     *  JsonObject.create()
     *    .put("type", "airline")
     *    .put("name", "A%")
     * )
     * ```
     *
     * Using parameterized statements combined with non-adhoc queries (which is configurable through
     * the {@link N1qlParams}) can provide better performance even when the actual arguments change
     * at execution time.
     *
     * @param statement the {@link Statement} to execute (containing named placeholders)
     * @param namedParams the values for the named placeholders in statement
     */
    public static ParameterizedN1qlQuery parameterized(Statement statement, JsonObject namedParams) {
        return new ParameterizedN1qlQuery(statement, namedParams, null);
    }

    /**
     * Create a new query with positionalParameters. Note that the {@link JsonArray}
     * should not be mutated until {@link #n1ql()} is called since it backs the
     * creation of the query string.
     *
     * Positional parameters have the form of `$n`, where the `n` represents the position, starting
     * with 1. The following two examples are equivalent and compare the {@link #simple(Statement)}
     * vs the positional {@link #parameterized(Statement, JsonArray)} approach:
     *
     * Simple:
     *
     * ```
     * N1qlQuery.simple("SELECT * FROM `travel-sample` WHERE type = 'airline' and name like 'A%'")
     * ```
     *
     * Positional Params:
     *
     * ```
     * N1qlQuery.parameterized(
     *  "SELECT * FROM `travel-sample` WHERE type = $1 and name like $2",
     *  JsonArray.from("airline", "A%")
     * )
     * ```
     *
     * Using parameterized statements combined with non-adhoc queries (which is configurable through
     * the {@link N1qlParams}) can provide better performance even when the actual arguments change
     * at execution time.
     *
     * @param statement the {@link Statement} to execute (containing positional placeholders)
     * @param positionalParams the values for the positional placeholders in statement
     * @param params the {@link N1qlParams query parameters}.
     */
    public static ParameterizedN1qlQuery parameterized(Statement statement, JsonArray positionalParams, N1qlParams params) {
        return new ParameterizedN1qlQuery(statement, positionalParams, params);
    }

    /**
     * Create a new query with named parameters. Note that the {@link JsonObject}
     * should not be mutated until {@link #n1ql()} is called since it backs the
     * creation of the query string.
     *
     * Named parameters have the form of `$name`, where the `name` represents the unique name. The
     * following two examples are equivalent and compare the {@link #simple(Statement)}
     * vs the named {@link #parameterized(Statement, JsonObject)} approach:
     *
     * Simple:
     *
     * ```
     * N1qlQuery.simple("SELECT * FROM `travel-sample` WHERE type = 'airline' and name like 'A%'")
     * ```
     *
     * Named Params:
     *
     * ```
     * N1qlQuery.parameterized(
     *  "SELECT * FROM `travel-sample` WHERE type = $type and name like $name",
     *  JsonObject.create()
     *    .put("type", "airline")
     *    .put("name", "A%")
     * )
     * ```
     *
     * Using parameterized statements combined with non-adhoc queries (which is configurable through
     * the {@link N1qlParams}) can provide better performance even when the actual arguments change
     * at execution time.
     *
     * @param statement the {@link Statement} to execute (containing named placeholders)
     * @param namedParams the values for the named placeholders in statement
     * @param params the {@link N1qlParams query parameters}.
     */
    public static ParameterizedN1qlQuery parameterized(Statement statement, JsonObject namedParams, N1qlParams params) {
        return new ParameterizedN1qlQuery(statement, namedParams, params);
    }

    //== PARAMETERIZED with raw String ==
    /**
     * Create a new query with positionalParameters. Note that the {@link JsonArray}
     * should not be mutated until {@link #n1ql()} is called since it backs the
     * creation of the query string.
     *
     * Positional parameters have the form of `$n`, where the `n` represents the position, starting
     * with 1. The following two examples are equivalent and compare the {@link #simple(Statement)}
     * vs the positional {@link #parameterized(Statement, JsonArray)} approach:
     *
     * Simple:
     *
     * ```
     * N1qlQuery.simple("SELECT * FROM `travel-sample` WHERE type = 'airline' and name like 'A%'")
     * ```
     *
     * Positional Params:
     *
     * ```
     * N1qlQuery.parameterized(
     *  "SELECT * FROM `travel-sample` WHERE type = $1 and name like $2",
     *  JsonArray.from("airline", "A%")
     * )
     * ```
     *
     * Using parameterized statements combined with non-adhoc queries (which is configurable through
     * the {@link N1qlParams}) can provide better performance even when the actual arguments change
     * at execution time.
     *
     * @param statement the raw statement to execute (containing positional placeholders)
     * @param positionalParams the values for the positional placeholders in statement
     */
    public static ParameterizedN1qlQuery parameterized(String statement, JsonArray positionalParams) {
        return new ParameterizedN1qlQuery(new RawStatement(statement), positionalParams, null);
    }

    /**
     * Create a new query with named parameters. Note that the {@link JsonObject}
     * should not be mutated until {@link #n1ql()} is called since it backs the
     * creation of the query string.
     *
     * Named parameters have the form of `$name`, where the `name` represents the unique name. The
     * following two examples are equivalent and compare the {@link #simple(Statement)}
     * vs the named {@link #parameterized(Statement, JsonObject)} approach:
     *
     * Simple:
     *
     * ```
     * N1qlQuery.simple("SELECT * FROM `travel-sample` WHERE type = 'airline' and name like 'A%'")
     * ```
     *
     * Named Params:
     *
     * ```
     * N1qlQuery.parameterized(
     *  "SELECT * FROM `travel-sample` WHERE type = $type and name like $name",
     *  JsonObject.create()
     *    .put("type", "airline")
     *    .put("name", "A%")
     * )
     * ```
     *
     * Using parameterized statements combined with non-adhoc queries (which is configurable through
     * the {@link N1qlParams}) can provide better performance even when the actual arguments change
     * at execution time.
     *
     * @param statement the raw statement to execute (containing named placeholders)
     * @param namedParams the values for the named placeholders in statement
     */
    public static ParameterizedN1qlQuery parameterized(String statement, JsonObject namedParams) {
        return new ParameterizedN1qlQuery(new RawStatement(statement), namedParams, null);
    }

    /**
     * Create a new query with positionalParameters. Note that the {@link JsonArray}
     * should not be mutated until {@link #n1ql()} is called since it backs the
     * creation of the query string.
     *
     * Positional parameters have the form of `$n`, where the `n` represents the position, starting
     * with 1. The following two examples are equivalent and compare the {@link #simple(Statement)}
     * vs the positional {@link #parameterized(Statement, JsonArray)} approach:
     *
     * Simple:
     *
     * ```
     * N1qlQuery.simple("SELECT * FROM `travel-sample` WHERE type = 'airline' and name like 'A%'")
     * ```
     *
     * Positional Params:
     *
     * ```
     * N1qlQuery.parameterized(
     *  "SELECT * FROM `travel-sample` WHERE type = $1 and name like $2",
     *  JsonArray.from("airline", "A%")
     * )
     * ```
     *
     * Using parameterized statements combined with non-adhoc queries (which is configurable through
     * the {@link N1qlParams}) can provide better performance even when the actual arguments change
     * at execution time.
     *
     * @param statement the raw statement to execute (containing positional placeholders)
     * @param positionalParams the values for the positional placeholders in statement
     * @param params the {@link N1qlParams query parameters}.
     */
    public static ParameterizedN1qlQuery parameterized(String statement, JsonArray positionalParams, N1qlParams params) {
        return new ParameterizedN1qlQuery(new RawStatement(statement), positionalParams, params);
    }

    /**
     * Create a new query with named parameters. Note that the {@link JsonObject}
     * should not be mutated until {@link #n1ql()} is called since it backs the
     * creation of the query string.
     *
     * Named parameters have the form of `$name`, where the `name` represents the unique name. The
     * following two examples are equivalent and compare the {@link #simple(Statement)}
     * vs the named {@link #parameterized(Statement, JsonObject)} approach:
     *
     * Simple:
     *
     * ```
     * N1qlQuery.simple("SELECT * FROM `travel-sample` WHERE type = 'airline' and name like 'A%'")
     * ```
     *
     * Named Params:
     *
     * ```
     * N1qlQuery.parameterized(
     *  "SELECT * FROM `travel-sample` WHERE type = $type and name like $name",
     *  JsonObject.create()
     *    .put("type", "airline")
     *    .put("name", "A%")
     * )
     * ```
     *
     * Using parameterized statements combined with non-adhoc queries (which is configurable through
     * the {@link N1qlParams}) can provide better performance even when the actual arguments change
     * at execution time.
     *
     * @param statement the raw statement to execute (containing named placeholders)
     * @param namedParams the values for the named placeholders in statement
     * @param params the {@link N1qlParams query parameters}.
     */
    public static ParameterizedN1qlQuery parameterized(String statement, JsonObject namedParams, N1qlParams params) {
        return new ParameterizedN1qlQuery(new RawStatement(statement), namedParams, params);
    }
}
