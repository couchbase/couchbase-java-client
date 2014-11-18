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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a JSON array that can be stored and loaded from Couchbase Server.
 *
 * If boxed return values are unboxed, the calling code needs to make sure to handle potential
 * {@link NullPointerException}s.
 *
 * The {@link JsonArray} is backed by a {@link List} and is intended to work similar to it API wise, but to only
 * allow to store such objects which can be represented by JSON.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class JsonArray extends JsonValue implements Iterable<Object> {

    /**
     * The backing list of the array.
     */
    private final List<Object> content;

    /**
     * Creates a new {@link JsonArray}.
     */
    private JsonArray() {
        content = new ArrayList<Object>();
    }

    /**
     * Creates a empty {@link JsonArray}.
     *
     * @return a empty {@link JsonArray}.
     */
    public static JsonArray empty() {
        return new JsonArray();
    }

    /**
     * Creates a empty {@link JsonArray}.
     *
     * @return a empty {@link JsonArray}.
     */
    public static JsonArray create() {
        return new JsonArray();
    }

    /**
     * Creates a new {@link JsonArray} and populates it with the values supplied.
     *
     * If the type is not supported, a {@link IllegalArgumentException} exception is thrown.
     * @param items the items to be stored in the {@link JsonArray}.
     * @return a populated {@link JsonArray}.
     */
    public static JsonArray from(Object... items) {
        JsonArray array = new JsonArray();
        for (Object item : items) {
            if (checkType(item)) {
                array.add(item);
            } else {
                throw new IllegalArgumentException("Unsupported type for JsonArray: " + item.getClass());
            }
        }
        return array;
    }

    /**
     * Retrieves the value by the position in the {@link JsonArray} and does not cast it.
     *
     * @param index the index of the value.
     * @return the value if found, or null otherwise.
     */
    public Object get(int index) {
        return content.get(index);
    }

    /**
     * Append an element to the {@link JsonArray}.
     *
     * Note that the type is checked and a {@link IllegalArgumentException} is thrown if not supported.
     *
     * @param value the value to append.
     * @return the {@link JsonArray}.
     */
    public JsonArray add(Object value) {
        if (checkType(value)) {
            content.add(value);
        } else {
            throw new IllegalArgumentException("Unsupported type for JsonArray: " + value.getClass());
        }
        return this;
    }

    /**
     * Append an {@link String} element to the {@link JsonArray}.
     *
     * @param value the value to append.
     * @return the {@link JsonArray}.
     */
    public JsonArray add(String value) {
        content.add(value);
        return this;
    }

    /**
     * Retrieves the value by the position in the {@link JsonArray} and casts it to {@link String}.
     *
     * @param index the index of the value.
     * @return the value if found, or null otherwise.
     */
    public String getString(int index) {
        return (String) content.get(index);
    }

    /**
     * Append an {@link Long} element to the {@link JsonArray}.
     *
     * @param value the value to append.
     * @return the {@link JsonArray}.
     */
    public JsonArray add(long value) {
        content.add(value);
        return this;
    }

    /**
     * Retrieves the value by the position in the {@link JsonArray} and casts it to {@link Long}.
     *
     * Note that if value was stored as another numerical type, some truncation or rounding may occur.
     *
     * @param index the index of the value.
     * @return the value if found, or null otherwise.
     */
    public Long getLong(int index) {
        Number n = (Number) content.get(index);
        if (n == null) {
            return null;
        } else if (n instanceof Long) {
            return (Long) n;
        } else {
            return n.longValue(); //autoboxing to Long
        }
    }

    /**
     * Append an {@link Integer} element to the {@link JsonArray}.
     *
     * @param value the value to append.
     * @return the {@link JsonArray}.
     */
    public JsonArray add(int value) {
        content.add(value);
        return this;
    }

    /**
     * Retrieves the value by the position in the {@link JsonArray} and casts it to {@link Integer}.
     *
     * Note that if value was stored as another numerical type, some truncation or rounding may occur.
     *
     * @param index the index of the value.
     * @return the value if found, or null otherwise.
     */
    public Integer getInt(int index) {
        Number n = (Number) content.get(index);
        if (n == null) {
            return null;
        } else if (n instanceof Integer) {
            return (Integer) n;
        } else {
            return n.intValue(); //autoboxing to Integer
        }
    }

    /**
     * Append an {@link Double} element to the {@link JsonArray}.
     *
     * @param value the value to append.
     * @return the {@link JsonArray}.
     */
    public JsonArray add(double value) {
        content.add(value);
        return this;
    }

    /**
     * Retrieves the value by the position in the {@link JsonArray} and casts it to {@link Double}.
     *
     * Note that if value was stored as another numerical type, some truncation or rounding may occur.
     *
     * @param index the index of the value.
     * @return the value if found, or null otherwise.
     */
    public Double getDouble(int index) {
        Number n = (Number) content.get(index);
        if (n == null) {
            return null;
        } else if (n instanceof Double) {
            return (Double) n;
        } else {
            return n.doubleValue(); //autoboxing to Double
        }
    }

    /**
     * Append an {@link Boolean} element to the {@link JsonArray}.
     *
     * @param value the value to append.
     * @return the {@link JsonArray}.
     */
    public JsonArray add(boolean value) {
        content.add(value);
        return this;
    }

    /**
     * Retrieves the value by the position in the {@link JsonArray} and casts it to {@link Boolean}.
     *
     * @param index the index of the value.
     * @return the value if found, or null otherwise.
     */
    public boolean getBoolean(int index) {
        return (Boolean) content.get(index);
    }

    /**
     * Append an {@link JsonObject} element to the {@link JsonArray}.
     *
     * @param value the value to append.
     * @return the {@link JsonArray}.
     */
    public JsonArray add(JsonObject value) {
        content.add(value);
        return this;
    }

    /**
     * Retrieves the value by the position in the {@link JsonArray} and casts it to {@link JsonObject}.
     *
     * @param index the index of the value.
     * @return the value if found, or null otherwise.
     */
    public JsonObject getObject(int index) {
        return (JsonObject) content.get(index);
    }

    /**
     * Append an {@link JsonArray} element to the {@link JsonArray}.
     *
     * @param value the value to append.
     * @return the {@link JsonArray}.
     */
    public JsonArray add(JsonArray value) {
        content.add(value);
        return this;
    }

    /**
     * Retrieves the value by the position in the {@link JsonArray} and casts it to {@link JsonArray}.
     *
     * @param index the index of the value.
     * @return the value if found, or null otherwise.
     */
    public JsonArray getArray(int index) {
        return (JsonArray) content.get(index);
    }

    /**
     * Copies the content of the {@link JsonArray} into a new {@link List} and return it.
     *
     * @return the content of the {@link JsonArray} in a new {@link List}.
     */
    public List<Object> toList() {
        return new ArrayList<Object>(content);
    }

    /**
     * Checks if the {@link JsonArray} is empty or not.
     *
     * @return true if it is, false otherwise.
     */
    public boolean isEmpty() {
        return content.isEmpty();
    }

    /**
     * Returns the size of the {@link JsonArray}.
     *
     * @return the size.
     */
    public int size() {
        return content.size();
    }

    @Override
    public Iterator<Object> iterator() {
        return content.iterator();
    }

    /**
     * Converts the {@link JsonArray} into its JSON string representation.
     *
     * @return the JSON string representing this {@link JsonArray}.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < content.size(); i++) {
            Object item = content.get(i);
            boolean isString = item instanceof String;

            if (isString) {
                sb.append("\"");
            }

            if (item == null) {
                sb.append("null");
            } else {
                sb.append(item.toString());
            }

            if (isString) {
                sb.append("\"");
            }
            if (i < content.size()-1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JsonArray array = (JsonArray) o;

        if (content != null ? !content.equals(array.content) : array.content != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }
}
