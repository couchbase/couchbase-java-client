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
 * DSL for N1QL functions in the misc/meta category.
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class MetaFunctions {

    private MetaFunctions() {}

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
