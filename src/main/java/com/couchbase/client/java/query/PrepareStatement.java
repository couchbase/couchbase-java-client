/**
 * Copyright (C) 2015 Couchbase, Inc.
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

import com.couchbase.client.java.util.DigestUtils;

/**
 * A PREPARE {@link Statement} that wraps another Statement in order to send it
 * to the server and produce a {@link PreparedPayload}.
 *
 * @author Simon Baslé
 * @since 2.1
 */
public class PrepareStatement implements SerializableStatement {

    /** a prefix to be used in order to prepare a named query plan for a statement */
    public static final String PREPARE_PREFIX = "PREPARE ";

    private static final long serialVersionUID = -3951622990579947393L;

    private final SerializableStatement toPrepare;
    private final String preparedName;

    private PrepareStatement(Statement toPrepare, String preparedName) {
        if (toPrepare instanceof SerializableStatement) {
            this.toPrepare = (SerializableStatement) toPrepare;
        } else {
            this.toPrepare = new N1qlQuery.RawStatement(toPrepare.toString());
        }
        this.preparedName = preparedName;
    }

    public Statement originalStatement() {
        return toPrepare;
    }

    public String preparedName() {
        return preparedName;
    }

    @Override
    public String toString() {
        if (preparedName != null) {
            return PREPARE_PREFIX + "`" + preparedName + "` FROM " + toPrepare.toString();
        }
        return PREPARE_PREFIX + toPrepare.toString();
    }

    /**
     * Construct a {@link PrepareStatement} from a select-like {@link Statement}.
     *
     * Note that passing a {@link PrepareStatement} will return it directly, whereas passing
     * a {@link PreparedPayload} will reuse the {@link PreparedPayload#preparedName()}.
     *
     * @param statement the {@link Statement} to prepare.
     * @return the prepared statement.
     * @throws IllegalArgumentException when statement cannot be prepared.
     */
    public static PrepareStatement prepare(Statement statement) {
        if (statement instanceof PrepareStatement) {
            return (PrepareStatement) statement;
        } else if (statement instanceof PreparedPayload) {
            PreparedPayload preparedPayload = (PreparedPayload) statement;
            return new PrepareStatement(preparedPayload.originalStatement(), preparedPayload.preparedName());
        } else {
            String preparedName = DigestUtils.digestSha1Hex(statement.toString());
            return new PrepareStatement(statement, preparedName);
        }
    }

    /**
     * Construct a {@link PrepareStatement} from a select-like {@link Statement} and give it a name.
     *
     * Note that passing a {@link PrepareStatement} or a {@link PreparedPayload} will reuse the
     * {@link PrepareStatement#originalStatement() original statement} but use the given name.
     *
     * @param statement the {@link Statement} to prepare.
     * @param preparedName the name to give to the prepared statement
     * @return the prepared statement.
     * @throws IllegalArgumentException when statement cannot be prepared.
     */
    public static PrepareStatement prepare(Statement statement, String preparedName) {
        Statement original;
        if (statement instanceof PrepareStatement) {
            original = ((PrepareStatement) statement).originalStatement();
        } else if (statement instanceof PreparedPayload) {
            original = ((PreparedPayload) statement).originalStatement();
        } else {
            original = statement;
        }
        return new PrepareStatement(original, preparedName);
    }

    /**
     * Construct a {@link PrepareStatement} from a statement in {@link String} format.
     * Statement shouldn't begin with "PREPARE", otherwise a syntax error may be returned by the server.
     *
     * @param statement the statement to prepare (not starting with "PREPARE").
     * @return the prepared statement.
     * @throws NullPointerException when statement is null.
     */
    public static PrepareStatement prepare(String statement) {
        if (statement == null) {
            throw new NullPointerException("Statement to prepare cannot be null");
        }
        //this is a guard against "PREPARE SELECT", but not enough anymore as it could be "PREPARE name FROM SELECT"...
        if (statement.startsWith(PREPARE_PREFIX)) {
            statement = statement.replaceFirst(PREPARE_PREFIX, "");
        }

        //delegate to other factory method so that a name is generated
        return prepare(new N1qlQuery.RawStatement(statement));
    }
}
