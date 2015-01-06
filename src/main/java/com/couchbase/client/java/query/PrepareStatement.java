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

/**
 * A PREPARE {@link Statement} that wraps another Statement in order to send it
 * to the server and produce a {@link QueryPlan}.
 *
 * @author Simon Baslé
 * @since 2.1
 */
public class PrepareStatement implements Statement {

    private final Statement toPrepare;

    private PrepareStatement(Statement toPrepare) {
        this.toPrepare = toPrepare;
    }

    @Override
    public String toString() {
        return "PREPARE " + toPrepare.toString();
    }

    /**
     * Construct a {@link PrepareStatement} from a select-like {@link Statement}.
     *
     * Note that passing a {@link PrepareStatement} will return it directly, whereas passing
     * a {@link QueryPlan} will result in an {@link IllegalArgumentException}.
     *
     * @param statement the {@link Statement} to prepare.
     * @return the prepared statement.
     * @throws IllegalArgumentException when statement cannot be prepared.
     */
    public static PrepareStatement prepare(Statement statement) {
        if (statement instanceof QueryPlan) {
            throw new IllegalArgumentException("QueryPlan cannot be prepared again");
        } else if (statement instanceof PrepareStatement) {
            return (PrepareStatement) statement;
        } else {
            return new PrepareStatement(statement);
        }
    }
}
