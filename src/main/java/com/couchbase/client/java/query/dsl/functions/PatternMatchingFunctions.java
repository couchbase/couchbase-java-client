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
 * DSL for N1QL functions in the Pattern Matching category.
 *
 * Pattern-matching functions allow you to work determine if strings contain a regular expression pattern.
 * You can also find the first position of a regular expression pattern and replace a regular expression with another.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class PatternMatchingFunctions {

    /**
     * Returned expression results in True if the string value contains the regular expression pattern.
     */
    public static Expression regexpContains(Expression expression, String pattern) {
        return x("REGEXP_CONTAINS(" + expression.toString() + ", \"" + pattern + "\")");
    }

    /**
     * Returned expression results in True if the string value contains the regular expression pattern.
     */
    public static Expression regexpContains(String expression, String pattern) {
        return regexpContains(x(expression), pattern);
    }

    /**
     * Returned expression results in True if the string value matches the regular expression pattern
     */
    public static Expression regexpLike(Expression expression, String pattern) {
        return x("REGEXP_LIKE(" + expression.toString() + ", \"" + pattern + "\")");
    }

    /**
     * Returned expression results in True if the string value matches the regular expression pattern
     */
    public static Expression regexpLike(String expression, String pattern) {
        return regexpLike(x(expression), pattern);
    }

    /**
     * Returned expression results in the first position of the regular expression pattern within the string, or -1.
     */
    public static Expression regexpPosition(Expression expression, String pattern) {
        return x("REGEXP_POSITION(" + expression.toString() + ", \"" + pattern + "\")");
    }

    /**
     * Returned expression results in the first position of the regular expression pattern within the string, or -1.
     */
    public static Expression regexpPosition(String expression, String pattern) {
        return regexpPosition(x(expression), pattern);
    }

    /**
     * Returned expression results in a new string with occurrences of pattern replaced with repl.
     * At most n replacements are performed.
     */
    public static Expression regexpReplace(Expression expression, String pattern, String repl, int n) {
        return x("REGEXP_REPLACE(" + expression.toString() + ", \"" + pattern + "\", \"" + repl + "\", " + n + ")");
    }

    /**
     * Returned expression results in a new string with occurrences of pattern replaced with repl.
     * At most n replacements are performed.
     */
    public static Expression regexpReplace(String expression, String pattern, String repl, int n) {
        return regexpReplace(x(expression), pattern, repl, n);
    }

    /**
     * Returned expression results in a new string with all occurrences of pattern replaced with repl.
     */
    public static Expression regexpReplace(Expression expression, String pattern, String repl) {
        return x("REGEXP_REPLACE(" + expression.toString() + ", \"" + pattern + "\", \"" + repl + "\")");
    }

    /**
     * Returned expression results in a new string with all occurrences of pattern replaced with repl.
     */
    public static Expression regexpReplace(String expression, String pattern, String repl) {
        return regexpReplace(x(expression), pattern, repl);
    }
}
