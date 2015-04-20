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
package com.couchbase.client.java.query.dsl.functions;

import static com.couchbase.client.java.query.dsl.Expression.x;

import com.couchbase.client.java.query.dsl.Expression;

/**
 * DSL for N1QL functions in the misc/meta category.
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.2
 */
public class MetaFunctions {

    /**
     * @return metadata for the document expression
     */
    public static Expression meta(Expression expression) {
        return x("META(" + expression.toString() + ")");
    }

    /**
     * @return metadata for the document expression
     */
    public static Expression meta(String expression) {
        return meta(x(expression));
    }

    /**
     * @return Base64 encoding of the expression, on the server side
     */
    public static Expression base64(Expression expression) {
        return x("BASE64(" + expression + ")");
    }

    /**
     * @return Base64 encoding of the expression, on the server side
     */
    public static Expression base64(String expression) {
        return base64(x(expression));
    }

    /**
     * @return a version 4 Universally Unique Identifier(UUID), generated on the server side
     */
    public static Expression uuid() {
        return x("UUID()");
    }
}
