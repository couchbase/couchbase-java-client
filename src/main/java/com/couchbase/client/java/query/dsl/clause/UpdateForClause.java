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

package com.couchbase.client.java.query.dsl.clause;

import static com.couchbase.client.java.query.dsl.Expression.x;

import java.util.ArrayList;

import com.couchbase.client.java.query.dsl.Expression;

/**
 * UpdateForClause is a clause used in N1QL Updates, more specifically in the "set" part. For example:
 *
 * <code>UPDATE bucket1 USE KEYS "abc123" SET "version.description" = "blabla" FOR variable IN path WHEN condition END;</code>.
 *
 * This Clause allows you to produce an {@link Expression} that corresponds to "<code>FOR variable IN path WHEN condition END</code>".
 *
 * @author Simon Baslé
 * @since 2.2
 */
public class UpdateForClause {

    private ArrayList<Expression> vars = new ArrayList<Expression>();

    private UpdateForClause() { }

    /**
     * Creates an updateFor clause that starts with <code>FOR variable IN path</code>.
     * @param variable the first variable in the clause.
     * @param path the first path in the clause, an IN path.
     * @return the clause, for chaining. See {@link #when(Expression)} and {@link #end()} to complete the clause.
     */
    public static UpdateForClause forIn(String variable, String path) {
        UpdateForClause clause = new UpdateForClause();
        return clause.in(variable, path);
    }

    /**
     * Creates an updateFor clause that starts with <code>FOR variable WITHIN path</code>.
     *
     * @param variable the first variable in the clause.
     * @param path the first path in the clause, a WITHIN path.
     * @return the clause, for chaining. See {@link #when(Expression)} and {@link #end()} to complete the clause.
     */
    public static UpdateForClause forWithin(String variable, String path) {
        UpdateForClause clause = new UpdateForClause();
        return clause.within(variable, path);
    }

    /**
     * Adds a "<code>variable IN path</code>" section to the clause.
     *
     * @param variable the next variable to add to the clause.
     * @param path the path for the variable, an IN path.
     * @return the clause, for chaining. See {@link #when(Expression)} and {@link #end()} to complete the clause.
     */
    public UpdateForClause in(String variable, String path) {
        Expression in = x(variable + " IN " + path);
        vars.add(in);
        return this;
    }

    /**
     * Adds a "<code>variable WITHIN path</code>" section to the clause.
     *
     * @param variable the next variable to add to the clause.
     * @param path the path for the variable, a WITHIN path.
     * @return the clause, for chaining. See {@link #when(Expression)} and {@link #end()} to complete the clause.
     */
    public UpdateForClause within(String variable, String path) {
        vars.add(x(variable + " WITHIN " + path));
        return this;
    }

    /**
     * Terminates the clause by adding a condition to it ("<code>WHEN condition END</code>") and
     * returns the corresponding {@link Expression}.
     *
     * @param condition the condition to add to the clause, in a WHEN section.
     * @return the {@link Expression} representing the updateFor clause.
     * @see #end() if you don't need a condition.
     */
    public Expression when(Expression condition) {
        StringBuilder updateFor = new StringBuilder("FOR ");
        for (Expression var : vars) {
            updateFor.append(var.toString()).append(", ");
        }
        updateFor.delete(updateFor.length() - 2, updateFor.length());
        if (condition != null) {
            updateFor.append(" WHEN ").append(condition.toString());
        }
        updateFor.append(" END");
        return x(updateFor.toString());
    }

    /**
     * Terminates the clause without a particular WHEN condition ("<code>END</code>") and
     * returns the corresponding {@link Expression}.
     *
     * @return the {@link Expression} representing the updateFor clause.
     * @see #when(Expression) if you need a condition.
     */
    public Expression end() {
        return when(null);
    }
}
