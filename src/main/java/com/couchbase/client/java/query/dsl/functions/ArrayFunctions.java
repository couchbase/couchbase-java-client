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

import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.dsl.Expression;

/**
 * DSL for N1QL functions in the Array category.
 *
 * You can use array functions to evaluate arrays, perform computations on elements
 * in an array, and to return a new array based on a transformation.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class ArrayFunctions {

    private ArrayFunctions() {}

    /**
     * Returned expression results in new array with value appended.
     */
    public static Expression arrayAppend(Expression expression, Expression value) {
        return x("ARRAY_APPEND(" + expression.toString() + ", " + value.toString() + ")");
    }

    /**
     * Returned expression results in new array with value appended.
     */
    public static Expression arrayAppend(String expression, Expression value) {
        return arrayAppend(x(expression), value);
    }

    /**
     * Returned expression results in new array with value appended.
     */
    public static Expression arrayAppend(JsonArray array, Expression value) {
        return arrayAppend(x(array), value);
    }

    /**
     * Returned expression results in arithmetic mean (average) of all the non-NULL number values in the array,
     * or NULL if there are no such values.
     */
    public static Expression arrayAvg(Expression expression) {
        return x("ARRAY_AVG(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in arithmetic mean (average) of all the non-NULL number values in the array,
     * or NULL if there are no such values.
     */
    public static Expression arrayAvg(String expression) {
        return arrayAvg(x(expression));
    }

    /**
     * Returned expression results in arithmetic mean (average) of all the non-NULL number values in the array,
     * or NULL if there are no such values.
     */
    public static Expression arrayAvg(JsonArray array) {
        return arrayAvg(x(array));
    }

    /**
     * Returned expression results in new array with the concatenation of the input arrays.
     */
    public static Expression arrayConcat(Expression expression1, Expression expression2) {
        return x("ARRAY_CONCAT(" + expression1.toString() + ", " + expression2.toString() + ")");
    }

    /**
     * Returned expression results in new array with the concatenation of the input arrays.
     */
    public static Expression arrayConcat(String expression1, String expression2) {
        return arrayConcat(x(expression1), x(expression2));
    }

    /**
     * Returned expression results in new array with the concatenation of the input arrays.
     */
    public static Expression arrayConcat(JsonArray array1, JsonArray array2) {
        return arrayConcat(x(array1), x(array2));
    }

    /**
     * Returned expression results in true if the array contains value.
     */
    public static Expression arrayContains(Expression expression, Expression value) {
        return x("ARRAY_CONTAINS(" + expression.toString() + ", " + value.toString() + ")");
    }

    /**
     * Returned expression results in true if the array contains value.
     */
    public static Expression arrayContains(String expression, Expression value) {
        return arrayContains(x(expression), value);
    }

    /**
     * Returned expression results in true if the array contains value.
     */
    public static Expression arrayContains(JsonArray array, Expression value) {
        return arrayContains(x(array), value);
    }

    /**
     * Returned expression results in count of all the non-NULL values in the array, or zero if there are no such values.
     */
    public static Expression arrayCount(Expression expression) {
        return x("ARRAY_COUNT(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in count of all the non-NULL values in the array, or zero if there are no such values.
     */
    public static Expression arrayCount(String expression) {
        return arrayCount(x(expression));
    }

    /**
     * Returned expression results in count of all the non-NULL values in the array, or zero if there are no such values.
     */
    public static Expression arrayCount(JsonArray array) {
        return arrayCount(x(array));
    }

    /**
     * Returned expression results in new array with distinct elements of input array.
     */
    public static Expression arrayDistinct(Expression expression) {
        return x("ARRAY_DISTINCT(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in new array with distinct elements of input array.
     */
    public static Expression arrayDistinct(String expression) {
        return arrayDistinct(x(expression));
    }

    /**
     * Returned expression results in new array with distinct elements of input array.
     */
    public static Expression arrayDistinct(JsonArray array) {
        return arrayDistinct(x(array));
    }

    /**
     * Returned expression results in the first non-NULL value in the array, or NULL.
     */
    public static Expression arrayIfNull(Expression expression) {
        return x("ARRAY_IFNULL(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the first non-NULL value in the array, or NULL.
     */
    public static Expression arrayIfNull(String expression) {
        return arrayIfNull(x(expression));
    }

    /**
     * Returned expression results in the first non-NULL value in the array, or NULL.
     */
    public static Expression arrayIfNull(JsonArray array) {
        return arrayIfNull(x(array));
    }

    /**
     * Returned expression results in the number of elements in the array.
     */
    public static Expression arrayLength(Expression expression) {
        return x("ARRAY_LENGTH(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the number of elements in the array.
     */
    public static Expression arrayLength(String expression) {
        return arrayLength(x(expression));
    }

    /**
     * Returned expression results in the number of elements in the array.
     */
    public static Expression arrayLength(JsonArray array) {
        return arrayLength(x(array));
    }

    /**
     * Returned expression results in the largest non-NULL, non-MISSING array element, in N1QL collation order.
     */
    public static Expression arrayMax(Expression expression) {
        return x("ARRAY_MAX(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the largest non-NULL, non-MISSING array element, in N1QL collation order.
     */
    public static Expression arrayMax(String expression) {
        return arrayMax(x(expression));
    }

    /**
     * Returned expression results in the largest non-NULL, non-MISSING array element, in N1QL collation order.
     */
    public static Expression arrayMax(JsonArray array) {
        return arrayMax(x(array));
    }

    /**
     * Returned expression results in the smallest non-NULL, non-MISSING array element, in N1QL collation order.
     */
    public static Expression arrayMin(Expression expression) {
        return x("ARRAY_MIN(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the smallest non-NULL, non-MISSING array element, in N1QL collation order.
     */
    public static Expression arrayMin(String expression) {
        return arrayMin(x(expression));
    }

    /**
     * Returned expression results in the smallest non-NULL, non-MISSING array element, in N1QL collation order.
     */
    public static Expression arrayMin(JsonArray array) {
        return arrayMin(x(array));
    }

    /**
     * Returned expression results in the first position of value within the array, or -1.
     * Array position is zero-based, i.e. the first position is 0.
     */
    public static Expression arrayPosition(Expression expression, Expression value) {
        return x("ARRAY_POSITION(" + expression.toString() + ", " + value.toString() + ")");
    }

    /**
     * Returned expression results in the first position of value within the array, or -1.
     * Array position is zero-based, i.e. the first position is 0.
     */
    public static Expression arrayPosition(String expression, Expression value) {
        return arrayPosition(x(expression), value);
    }

    /**
     * Returned expression results in the first position of value within the array, or -1.
     * Array position is zero-based, i.e. the first position is 0.
     */
    public static Expression arrayPosition(JsonArray array, Expression value) {
        return arrayPosition(x(array), value);
    }

    /**
     * Returned expression results in the new array with value pre-pended.
     */
    public static Expression arrayPrepend(Expression expression, Expression value) {
        return x("ARRAY_PREPEND(" + value.toString() + ", " + expression.toString() + ")");
    }

    /**
     * Returned expression results in the new array with value pre-pended.
     */
    public static Expression arrayPrepend(String expression, Expression value) {
        return arrayPrepend(x(expression), value);
    }

    /**
     * Returned expression results in the new array with value pre-pended.
     */
    public static Expression arrayPrepend(JsonArray array, Expression value) {
        return arrayPrepend(x(array), value);
    }

    /**
     * Returned expression results in new array with value appended, if value is not already present,
     * otherwise the unmodified input array.
     */
    public static Expression arrayPut(Expression expression, Expression value) {
        return x("ARRAY_PUT(" + expression.toString() + ", " + value.toString() + ")");
    }

    /**
     * Returned expression results in new array with value appended, if value is not already present,
     * otherwise the unmodified input array.
     */
    public static Expression arrayPut(String expression, Expression value) {
        return arrayPut(x(expression), value);
    }

    /**
     * Returned expression results in new array with value appended, if value is not already present,
     * otherwise the unmodified input array.
     */
    public static Expression arrayPut(JsonArray array, Expression value) {
        return arrayPut(x(array), value);
    }

    /**
     * Returned expression results in new array of numbers, from start until the largest number less than end.
     * Successive numbers are incremented by step. If step is omitted, the default is 1. If step is negative,
     * decrements until the smallest number greater than end.
     */
    public static Expression arrayRange(long start, long end, long step) {
        return x("ARRAY_RANGE(" + start + ", " + end + ", " + step + ")");
    }

    /**
     * Returned expression results in new array of numbers, from start until the largest number less than end.
     * Successive numbers are incremented by 1.
     */
    public static Expression arrayRange(long start, long end) {
        return x("ARRAY_RANGE(" + start + ", " + end + ")");
    }

    /**
     * Returned expression results in new array with all occurrences of value removed.
     */
    public static Expression arrayRemove(Expression expression, Expression value) {
        return x("ARRAY_REMOVE(" + expression.toString() + ", " + value.toString() + ")");
    }

    /**
     * Returned expression results in new array with all occurrences of value removed.
     */
    public static Expression arrayRemove(String expression, Expression value) {
        return arrayRemove(x(expression), value);
    }

    /**
     * Returned expression results in new array with all occurrences of value removed.
     */
    public static Expression arrayRemove(JsonArray array, Expression value) {
        return arrayRemove(x(array), value);
    }

    /**
     * Returned expression results in new array with value repeated n times.
     */
    public static Expression arrayRepeat(Expression value, long n) {
        return x("ARRAY_REPEAT(" + value.toString() + ", " + n + ")");
    }

    /**
     * Returned expression results in new array with the string "value" repeated n times.
     */
    public static Expression arrayRepeat(String value, long n) {
        return arrayRepeat(s(value), n);
    }

    /**
     * Returned expression results in new array with value repeated n times.
     */
    public static Expression arrayRepeat(Number value, long n) {
        return arrayRepeat(x(value), n);
    }

    /**
     * Returned expression results in new array with value repeated n times.
     */
    public static Expression arrayRepeat(boolean value, long n) {
        return arrayRepeat(x(value), n);
    }

    /**
     * Returned expression results in new array with all occurrences of value1 replaced by value2.
     */

    public static Expression arrayReplace(Expression expression, Expression value1, Expression value2) {
        return x("ARRAY_REPLACE(" + expression.toString() + ", " + value1 + ", " + value2 + ")");
    }

    /**
     * Returned expression results in new array with all occurrences of value1 replaced by value2.
     */

    public static Expression arrayReplace(String expression, Expression value1, Expression value2) {
        return arrayReplace(x(expression), value1, value2);
    }

    /**
     * Returned expression results in new array with all occurrences of value1 replaced by value2.
     */

    public static Expression arrayReplace(JsonArray array, Expression value1, Expression value2) {
        return arrayReplace(x(array), value1, value2);
    }

    /**
     * Returned expression results in new array with at most n occurrences of value1 replaced with value2.
     */
    public static Expression arrayReplace(Expression expression, Expression value1, Expression value2, long n) {
        return x("ARRAY_REPLACE(" + expression.toString() + ", " + value1 + ", " + value2 + ", " + n + ")");
    }

    /**
     * Returned expression results in new array with at most n occurrences of value1 replaced with value2.
     */
    public static Expression arrayReplace(String expression, Expression value1, Expression value2, long n) {
        return arrayReplace(x(expression), value1, value2, n);
    }

    /**
     * Returned expression results in new array with at most n occurrences of value1 replaced with value2.
     */
    public static Expression arrayReplace(JsonArray array, Expression value1, Expression value2, long n) {
        return arrayReplace(x(array), value1, value2, n);
    }

    /**
     * Returned expression results in new array with all elements in reverse order.
     */
    public static Expression arrayReverse(Expression expression) {
        return x("ARRAY_REVERSE(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in new array with all elements in reverse order.
     */
    public static Expression arrayReverse(String expression) {
        return arrayReverse(x(expression));
    }

    /**
     * Returned expression results in new array with all elements in reverse order.
     */
    public static Expression arrayReverse(JsonArray array) {
        return arrayReverse(x(array));
    }

    /**
     * Returned expression results in new array with elements sorted in N1QL collation order.
     */
    public static Expression arraySort(Expression expression) {
        return x("ARRAY_SORT(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in new array with elements sorted in N1QL collation order.
     */
    public static Expression arraySort(String expression) {
        return arraySort(x(expression));
    }

    /**
     * Returned expression results in new array with elements sorted in N1QL collation order.
     */
    public static Expression arraySort(JsonArray array) {
        return arraySort(x(array));
    }

    /**
     * Returned expression results in the sum of all the non-NULL number values in the array,
     * or zero if there are no such values.
     */
    public static Expression arraySum(Expression expression) {
        return x("ARRAY_SUM(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the sum of all the non-NULL number values in the array,
     * or zero if there are no such values.
     */
    public static Expression arraySum(String expression) {
        return arraySum(x(expression));
    }

    /**
     * Returned expression results in the sum of all the non-NULL number values in the array,
     * or zero if there are no such values.
     */
    public static Expression arraySum(JsonArray array) {
        return arraySum(x(array));
    }
}
