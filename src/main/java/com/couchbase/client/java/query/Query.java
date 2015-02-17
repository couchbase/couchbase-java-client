/**
 * Copyright (C) 2014 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
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
public abstract class Query implements Serializable {

    private static final long serialVersionUID = 3758119606237959729L;

    /**
     * Returns the {@link Statement} from this query. Note that this is the only mandatory
     * part of a N1QL query.
     *
     * @return the statement that forms the base of this query
     */
    public abstract Statement statement();

    /**
     * Returns the {@link QueryParams} representing customization of the N1QL query.
     *
     * Note that this is different from named or positional parameters (which relate to the statement).
     *
     * @return the {@link QueryParams} for this query, null if none.
     */
    public abstract QueryParams params();

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
     * Create a new {@link Query} with a plain un-parametrized {@link Statement}.
     *
     * @param statement the {@link Statement} to execute
     */
    public static SimpleQuery simple(Statement statement) {
        return new SimpleQuery(statement, null);
    }

    /**
     * Create a new {@link Query} with a plain raw statement in String form.
     *
     * @param statement the raw statement string to execute (eg. "SELECT * FROM default").
     */
    public static SimpleQuery simple(String statement) {
        return simple(new RawStatement(statement));
    }

    /**
     * Create a new {@link Query} with a plain un-parametrized {@link Statement} and
     * custom query parameters.
     *
     * @param statement the {@link Statement} to execute
     * @param params the {@link QueryParams query parameters}.
     */
    public static SimpleQuery simple(Statement statement, QueryParams params) {
        return new SimpleQuery(statement, params);
    }

    /**
     * Create a new {@link Query} with a plain raw statement in {@link String} form and
     * custom query parameters.
     *
     * @param statement the raw statement string to execute (eg. "SELECT * FROM default").
     * @param params the {@link QueryParams query parameters}.
     */
    public static SimpleQuery simple(String statement, QueryParams params) {
        return simple(new RawStatement(statement), params);
    }

    //== PARAMETRIZED with Statement ==
    /**
     * Create a new query with positionalParameters. Note that the {@link JsonArray}
     * should not be mutated until {@link #n1ql()} is called since it backs the
     * creation of the query string.
     *
     * @param statement the {@link Statement} to execute (containing positional placeholders)
     * @param positionalParams the values for the positional placeholders in statement
     */
    public static ParametrizedQuery parametrized(Statement statement, JsonArray positionalParams) {
        return new ParametrizedQuery(statement, positionalParams, null);
    }

    /**
     * Create a new query with named parameters. Note that the {@link JsonObject}
     * should not be mutated until {@link #n1ql()} is called since it backs the
     * creation of the query string.
     *
     * @param statement the {@link Statement} to execute (containing named placeholders)
     * @param namedParams the values for the named placeholders in statement
     */
    public static ParametrizedQuery parametrized(Statement statement, JsonObject namedParams) {
        return new ParametrizedQuery(statement, namedParams, null);
    }

    /**
     * Create a new query with positionalParameters. Note that the {@link JsonArray}
     * should not be mutated until {@link #n1ql()} is called since it backs the
     * creation of the query string.
     *
     * @param statement the {@link Statement} to execute (containing positional placeholders)
     * @param positionalParams the values for the positional placeholders in statement
     * @param params the {@link QueryParams query parameters}.
     */
    public static ParametrizedQuery parametrized(Statement statement, JsonArray positionalParams, QueryParams params) {
        return new ParametrizedQuery(statement, positionalParams, params);
    }

    /**
     * Create a new query with named parameters. Note that the {@link JsonObject}
     * should not be mutated until {@link #n1ql()} is called since it backs the
     * creation of the query string.
     *
     * @param statement the {@link Statement} to execute (containing named placeholders)
     * @param namedParams the values for the named placeholders in statement
     * @param params the {@link QueryParams query parameters}.
     */
    public static ParametrizedQuery parametrized(Statement statement, JsonObject namedParams, QueryParams params) {
        return new ParametrizedQuery(statement, namedParams, params);
    }

    //== PARAMETRIZED with raw String ==
    /**
     * Create a new query with positionalParameters. Note that the {@link JsonArray}
     * should not be mutated until {@link #n1ql()} is called since it backs the
     * creation of the query string.
     *
     * @param statement the raw statement to execute (containing positional placeholders)
     * @param positionalParams the values for the positional placeholders in statement
     */
    public static ParametrizedQuery parametrized(String statement, JsonArray positionalParams) {
        return new ParametrizedQuery(new RawStatement(statement), positionalParams, null);
    }

    /**
     * Create a new query with named parameters. Note that the {@link JsonObject}
     * should not be mutated until {@link #n1ql()} is called since it backs the
     * creation of the query string.
     *
     * @param statement the raw statement to execute (containing named placeholders)
     * @param namedParams the values for the named placeholders in statement
     */
    public static ParametrizedQuery parametrized(String statement, JsonObject namedParams) {
        return new ParametrizedQuery(new RawStatement(statement), namedParams, null);
    }

    /**
     * Create a new query with positionalParameters. Note that the {@link JsonArray}
     * should not be mutated until {@link #n1ql()} is called since it backs the
     * creation of the query string.
     *
     * @param statement the raw statement to execute (containing positional placeholders)
     * @param positionalParams the values for the positional placeholders in statement
     * @param params the {@link QueryParams query parameters}.
     */
    public static ParametrizedQuery parametrized(String statement, JsonArray positionalParams, QueryParams params) {
        return new ParametrizedQuery(new RawStatement(statement), positionalParams, params);
    }

    /**
     * Create a new query with named parameters. Note that the {@link JsonObject}
     * should not be mutated until {@link #n1ql()} is called since it backs the
     * creation of the query string.
     *
     * @param statement the raw statement to execute (containing named placeholders)
     * @param namedParams the values for the named placeholders in statement
     * @param params the {@link QueryParams query parameters}.
     */
    public static ParametrizedQuery parametrized(String statement, JsonObject namedParams, QueryParams params) {
        return new ParametrizedQuery(new RawStatement(statement), namedParams, params);
    }

    //== PREPARED ==
    /**
     * Create a new prepared query without parameters (the original statement shouldn't contain
     * parameter placeholders).
     *
     * @param plan the prepared {@link QueryPlan} to execute (containing no placeholders).
     */
    public static PreparedQuery prepared(QueryPlan plan) {
        return new PreparedQuery(plan, null);
    }

    /**
     * Create a new prepared query with positionalParameters. Note that the {@link JsonArray}
     * should not be mutated until {@link #n1ql()} is called since it backs the
     * creation of the query string.
     *
     * @param plan the prepared {@link QueryPlan} to execute (containing positional placeholders).
     * @param positionalParams the values for the positional placeholders in statement.
     */
    public static PreparedQuery prepared(QueryPlan plan, JsonArray positionalParams) {
        return new PreparedQuery(plan, positionalParams, null);
    }

    /**
     * Create a new prepared query with named parameters. Note that the {@link JsonObject}
     * should not be mutated until {@link #n1ql()} is called since it backs the
     * creation of the query string.
     *
     * @param plan the prepared {@link QueryPlan} to execute (containing named placeholders).
     * @param namedParams the values for the named placeholders in statement.
     */
    public static PreparedQuery prepared(QueryPlan plan, JsonObject namedParams) {
        return new PreparedQuery(plan, namedParams, null);
    }

    /**
     * Create a new prepared query without parameters (the original statement shouldn't contain
     * parameter placeholders).
     *
     * @param plan the prepared {@link QueryPlan} to execute (containing no placeholders).
     * @param params the {@link QueryParams query parameters}.
     */
    public static PreparedQuery prepared(QueryPlan plan, QueryParams params) {
        return new PreparedQuery(plan, params);
    }

    /**
     * Create a new prepared query with positionalParameters. Note that the {@link JsonArray}
     * should not be mutated until {@link #n1ql()} is called since it backs the
     * creation of the query string.
     *
     * @param plan the prepared {@link QueryPlan} to execute (containing positional placeholders).
     * @param positionalParams the values for the positional placeholders in statement.
     * @param params the {@link QueryParams query parameters}.
     */
    public static PreparedQuery prepared(QueryPlan plan, JsonArray positionalParams, QueryParams params) {
        return new PreparedQuery(plan, positionalParams, params);
    }

    /**
     * Create a new prepared query with named parameters. Note that the {@link JsonObject}
     * should not be mutated until {@link #n1ql()} is called since it backs the
     * creation of the query string.
     *
     * @param plan the prepared {@link QueryPlan} to execute (containing named placeholders).
     * @param namedParams the values for the named placeholders in statement.
     * @param params the {@link QueryParams query parameters}.
     */
    public static PreparedQuery prepared(QueryPlan plan, JsonObject namedParams, QueryParams params) {
        return new PreparedQuery(plan, namedParams, params);
    }

}
