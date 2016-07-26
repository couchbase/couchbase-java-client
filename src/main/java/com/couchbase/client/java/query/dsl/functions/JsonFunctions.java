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
import com.couchbase.client.deps.com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.couchbase.client.java.CouchbaseAsyncBucket;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.dsl.Expression;

/**
 * DSL for N1QL functions in the JSON category.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class JsonFunctions {

    private JsonFunctions() {}

    /**
     * The returned Expression unmarshals the expression representing a JSON-encoded string
     * into a N1QL value. The empty string results in MISSING.
     */
    public static Expression decodeJson(Expression expression) {
        return x("DECODE_JSON(" + expression.toString() + ")");
    }

    /**
     * The returned Expression unmarshals the JSON constant
     * into a N1QL value. The empty string results in MISSING.
     */
    public static Expression decodeJson(JsonObject json) {
        char[] encoded = JsonStringEncoder.getInstance().quoteAsString(json.toString());
        return x("DECODE_JSON(\"" + new String(encoded) + "\")");
    }

    /**
     * The returned Expression unmarshals the JSON-encoded string
     * into a N1QL value. The empty string results in MISSING.
     *
     * The string must represent JSON object marshalled into a JSON String,
     * eg. "{\"test\": true}" (excluding surrounding quotes).
     */
    public static Expression decodeJson(String jsonString) {
        try {
            JsonObject jsonObject = CouchbaseAsyncBucket.JSON_OBJECT_TRANSCODER.stringToJsonObject(jsonString);
            return decodeJson(jsonObject);
        } catch (Exception e) {
            throw new IllegalArgumentException("String is not representing JSON object: " + jsonString);
        }
    }

    /**
     * Returned expression marshals the N1QL value into a JSON-encoded string. MISSING becomes the empty string.
     */
    public static Expression encodeJson(Expression expression) {
        return x("ENCODE_JSON(" + expression.toString() + ")");
    }

    /**
     * Returned expression marshals the N1QL value into a JSON-encoded string. MISSING becomes the empty string.
     */
    public static Expression encodeJson(String expression) {
        return encodeJson(x(expression));
    }

    /**
     * Returned expression results in the number of bytes in an uncompressed JSON encoding of the value.
     * The exact size is implementation-dependent. Always returns an integer, and never MISSING or NULL.
     * Returns 0 for MISSING.
     */
    public static Expression encodedSize(Expression expression) {
        return x("ENCODED_SIZE(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the number of bytes in an uncompressed JSON encoding of the value.
     * The exact size is implementation-dependent. Always returns an integer, and never MISSING or NULL.
     * Returns 0 for MISSING.
     */
    public static Expression encodedSize(String expression) {
        return encodedSize(x(expression));
    }

    /**
     * Returned expression results in length of the value after evaluating the expression.
     * The exact meaning of length depends on the type of the value:
     *
     *  - MISSING: MISSING
     *  - NULL: NULL
     *  - String: The length of the string.
     *  - Array: The number of elements in the array.
     *  - Object: The number of name/value pairs in the object
     *  - Any other value: NULL.
     */
    public static Expression polyLength(Expression expression) {
        return x("POLY_LENGTH(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in length of the value after evaluating the expression.
     * The exact meaning of length depends on the type of the value:
     *
     *  - MISSING: MISSING
     *  - NULL: NULL
     *  - String: The length of the string.
     *  - Array: The number of elements in the array.
     *  - Object: The number of name/value pairs in the object
     *  - Any other value: NULL.
     */
    public static Expression polyLength(String expression) {
        return polyLength(x(expression));
    }

}
