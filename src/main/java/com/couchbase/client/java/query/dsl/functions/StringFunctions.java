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

import static com.couchbase.client.java.query.dsl.Expression.sub;
import static com.couchbase.client.java.query.dsl.Expression.x;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.query.dsl.Expression;

/**
 * DSL for N1QL functions in the Strings category.
 *
 * String functions perform operations on a string input value and returns a string or other value.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class StringFunctions {

    private StringFunctions() {}

    /**
     * Returned expression results in True if the string expression contains the substring.
     */
    public static Expression contains(Expression expression, String substring) {
        return x("CONTAINS(" + expression.toString() + ", \"" + substring + "\")");
    }

    /**
     * Returned expression results in True if the string expression contains the substring.
     */
    public static Expression contains(String expression, String substring) {
        return contains(x(expression), substring);
    }

    /**
     * Returned expression results in the conversion of the string so that the first letter
     * of each word is uppercase and every other letter is lowercase.
     */
    public static Expression initCap(Expression expression) {
        return x("INITCAP(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the conversion of the string so that the first letter
     * of each word is uppercase and every other letter is lowercase.
     */
    public static Expression initCap(String expression) {
        return initCap(x(expression));
    }

    /**
     * Returned expression results in the conversion of the string so that the first letter
     * of each word is uppercase and every other letter is lowercase.
     */
    public static Expression title(Expression expression) {
        return x("TITLE(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the conversion of the string so that the first letter
     * of each word is uppercase and every other letter is lowercase.
     */
    public static Expression title(String expression) {
        return title(x(expression));
    }

    /**
     * Returned expression results in the length of the string expression.
     */
    public static Expression length(Expression expression) {
        return x("LENGTH(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the length of the string expression.
     */
    public static Expression length(String expression) {
        return length(x(expression));
    }

    /**
     * Returned expression results in the given string expression in lowercase
     */
    public static Expression lower(Expression expression) {
        return x("LOWER(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the string value for the given identifier, in lowercase
     */
    public static Expression lower(String identifier) {
        return lower(x(identifier));
    }

    /**
     * Returned expression results in the string with all leading white spaces removed.
     */
    public static Expression ltrim(Expression expression) {
        return x("LTRIM(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the string with all leading white spaces removed.
     */
    public static Expression ltrim(String expression) {
        return ltrim(x(expression));
    }

    /**
     * Returned expression results in the string with all leading chars removed (any char in the characters string).
     */
    public static Expression ltrim(Expression expression, String characters) {
        return x("LTRIM(" + expression.toString() + ", \"" + characters + "\")");
    }

    /**
     * Returned expression results in the string with all leading chars removed (any char in the characters string).
     */
    public static Expression ltrim(String expression, String characters) {
        return ltrim(x(expression), characters);
    }

    /**
     * Returned expression results in the first position of the substring within the string, or -1.
     * The position is zero-based, i.e., the first position is 0.
     */
    public static Expression position(Expression expression, String substring) {
        return x("POSITION(" + expression.toString() + ", \"" + substring + "\")");
    }

    /**
     * Returned expression results in the first position of the substring within the string, or -1.
     * The position is zero-based, i.e., the first position is 0.
     */
    public static Expression position(String expression, String substring) {
        return position(x(expression), substring);
    }

    /**
     * Returned expression results in the string formed by repeating expression n times.
     */
    public static Expression repeat(Expression expression, int n) {
        return x("REPEAT(" + expression.toString() + ", " + n + ")");
    }

    /**
     * Returned expression results in the string formed by repeating expression n times.
     */
    public static Expression repeat(String expression, int n) {
        return repeat(x(expression), n);
    }
//    REPLACE(expression, substring, repl [, n ])

    /**
     * Returned expression results in a string with all occurrences of substr replaced with repl.
     */
    public static Expression replace(Expression expression, String substring, String repl) {
        return x("REPLACE(" + expression.toString() + ", \"" + substring + "\", \"" + repl + "\")");
    }

    /**
     * Returned expression results in a string with all occurrences of substr replaced with repl.
     */
    public static Expression replace(String expression, String substring, String repl) {
        return replace(x(expression), substring, repl);
    }

    /**
     * Returned expression results in a string with at most n occurrences of substr replaced with repl.
     */
    public static Expression replace(Expression expression, String substring, String repl, int n) {
        return x("REPLACE(" + expression.toString() + ", \"" + substring + "\", \"" + repl + "\", " + n + ")");
    }

    /**
     * Returned expression results in a string with at most n occurrences of substr replaced with repl.
     */
    public static Expression replace(String expression, String substring, String repl, int n) {
        return replace(x(expression), substring, repl, n);
    }

    /**
     * Returned expression results in the string with all trailing white spaces removed.
     */
    public static Expression rtrim(Expression expression) {
        return x("RTRIM(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the string with all trailing white spaces removed.
     */
    public static Expression rtrim(String expression) {
        return rtrim(x(expression));
    }

    /**
     * Returned expression results in the string with all trailing chars removed (any char in the characters string).
     */
    public static Expression rtrim(Expression expression, String characters) {
        return x("RTRIM(" + expression.toString() + ", \"" + characters + "\")");
    }

    /**
     * Returned expression results in the string with all trailing chars removed (any char in the characters string).
     */
    public static Expression rtrim(String expression, String characters) {
        return rtrim(x(expression), characters);
    }

    /**
     * Returned expression results in a split of the string into an array of substrings
     * separated by any combination of white space characters.
     */
    public static Expression split(Expression expression) {
        return x("SPLIT(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in a split of the string into an array of substrings
     * separated by any combination of white space characters.
     */
    public static Expression split(String expression) {
        return split(x(expression));
    }

    /**
     * Returned expression results in a split of the string into an array of substrings separated by sep.
     */
    public static Expression split(Expression expression, String sep) {
        return x("SPLIT(" + expression.toString() + ", \"" + sep + "\")");
    }

    /**
     * Returned expression results in a split of the string into an array of substrings separated by sep.
     */
    public static Expression split(String expression, String sep) {
        return split(x(expression), sep);
    }

    /**
     * Returned expression results in a substring from the integer position of the given length.
     *
     * The position is zero-based, i.e. the first position is 0.
     * If position is negative, it is counted from the end of the string; -1 is the last position in the string.
     */
    public static Expression substr(Expression expression, int position, int length) {
        return x("SUBSTR(" + expression.toString() + ", " + position + ", " + length + ")");
    }

    /**
     * Returned expression results in a substring from the integer position of the given length.
     *
     * The position is zero-based, i.e. the first position is 0.
     * If position is negative, it is counted from the end of the string; -1 is the last position in the string.
     */
    public static Expression substr(String expression, int position, int length) {
        return x("SUBSTR(" + expression.toString() + ", " + position + ", " + length + ")");
    }

    /**
     * Returned expression results in a substring from the integer position to the end of the string.
     *
     * The position is zero-based, i.e. the first position is 0.
     * If position is negative, it is counted from the end of the string; -1 is the last position in the string.
     */
    public static Expression substr(Expression expression, int position) {
        return x("SUBSTR(" + expression.toString() + ", " + position + ")");
    }

    /**
     * Returned expression results in a substring from the integer position to the end of the string.
     *
     * The position is zero-based, i.e. the first position is 0.
     * If position is negative, it is counted from the end of the string; -1 is the last position in the string.
     */
    public static Expression substr(String expression, int position) {
        return x("SUBSTR(" + expression.toString() + ", " + position + ")");
    }

//            TRIM(expression [, characters ])
    /**
     * Returned expression results in the string with all leading and trailing white spaces removed.
     */
    public static Expression trim(Expression expression) {
        return x("TRIM(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the string with all leading and trailing white spaces removed.
     */
    public static Expression trim(String expression) {
        return trim(x(expression));
    }

    /**
     * Returned expression results in the string with all leading and trailing chars removed
     * (any char in the characters string).
     */
    public static Expression trim(Expression expression, String characters) {
        return x("TRIM(" + expression.toString() + ", \"" + characters + "\")");
    }

    /**
     * Returned expression results in the string with all leading and trailing chars removed
     * (any char in the characters string).
     */
    public static Expression trim(String expression, String characters) {
        return trim(x(expression), characters);
    }

    /**
     * Returned expression results in uppercase of the string expression.
     */
    public static Expression upper(Expression expression) {
        return x("UPPER(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in uppercase of the string expression.
     */
    public static Expression upper(String expression) {
        return upper(x(expression));
    }
}
