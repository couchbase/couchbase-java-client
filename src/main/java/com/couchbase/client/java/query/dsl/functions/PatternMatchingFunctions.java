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

    private PatternMatchingFunctions() {}

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
