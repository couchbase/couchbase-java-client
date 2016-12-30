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
package com.couchbase.client.java.document.json;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Represents a JSON value (either a {@link JsonObject} or a {@link JsonArray}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public abstract class JsonValue {

    /**
     * Represents a Json "null".
     */
    public static JsonNull NULL = JsonNull.INSTANCE;

    /**
     * Static factory method to create an empty {@link JsonObject}.
     *
     * @return an empty {@link JsonObject}.
     */
    public static JsonObject jo() {
        return JsonObject.create();
    }

    /**
     * Static factory method to create an empty {@link JsonArray}.
     *
     * @return an empty {@link JsonArray}.
     */
    public static JsonArray ja() {
        return JsonArray.create();
    }

    /**
     * Helper method to check if the given item is a supported JSON item.
     *
     * @param item the value to check.
     * @return true if supported, false otherwise.
     */
    public static boolean checkType(Object item) {
        return item == null
            || item instanceof String
            || item instanceof Integer
            || item instanceof Long
            || item instanceof Double
            || item instanceof Boolean
            || item instanceof BigInteger
            || item instanceof BigDecimal
            || item instanceof JsonObject
            || item instanceof JsonArray;
    }

}
