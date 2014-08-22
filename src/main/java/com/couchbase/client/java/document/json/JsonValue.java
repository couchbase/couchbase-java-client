/**
 * Copyright (C) 2014 Couchbase, Inc.
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
package com.couchbase.client.java.document.json;

/**
 * Represents a JSON value (either a {@link JsonObject} or a {@link JsonArray}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public abstract class JsonValue {

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
    protected static boolean checkType(Object item) {
        return item instanceof String
            || item instanceof Integer
            || item instanceof Long
            || item instanceof Double
            || item instanceof Boolean
            || item instanceof JsonObject
            || item instanceof JsonArray;
    }

}
