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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.query.dsl.Expression;

/**
 * DSL for N1QL functions in the Strings category.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class StringFunctions {

    /** @return the given string expression in lowercase */
    public static Expression lower(Expression expression) {
        return x("LOWER(" + expression.toString() + ")");
    }

    /** @return the string value for the given identifier, in lowercase */
    public static Expression lower(String identifier) {
        return lower(x(identifier));
    }

    /**
     * Extract a substring from the string expression.
     *
     * @param expression the string expression
     * @param position the starting position (0-indexed). Use a negative value to offset from the end of the string.
     * @param length number of chars to include
     */
    public static Expression substr(Expression expression, int position, int length) {
        return x("SUBSTR(" + expression.toString() + ", " + position + ", " + length + ")");
    }

    /**
     * Extract a substring from the string expression.
     *
     * @param expression the string expression
     * @param position the starting position (0-indexed). Use a negative value to offset from the end of the string.
     * @param length number of chars to include
     */
    public static Expression substr(String expression, int position, int length) {
        return x("SUBSTR(" + expression.toString() + ", " + position + ", " + length + ")");
    }

    /**
     * Extract a substring from the string expression, going from *position* to the end of the string.
     *
     * @param expression the string expression
     * @param position the starting position (0-indexed). Use a negative value to offset from the end of the string.
     */
    public static Expression substr(Expression expression, int position) {
        return x("SUBSTR(" + expression.toString() + ", " + position + ")");
    }

    /**
     * Extract a substring from the string expression, going from *position* to the end of the string.
     *
     * @param expression the string expression
     * @param position the starting position (0-indexed). Use a negative value to offset from the end of the string.
     */
    public static Expression substr(String expression, int position) {
        return x("SUBSTR(" + expression.toString() + ", " + position + ")");
    }



}
