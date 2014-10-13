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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a JSON object that can be stored and loaded from Couchbase Server.
 *
 * If boxed return values are unboxed, the calling code needs to make sure to handle potential
 * {@link NullPointerException}s.
 *
 * The {@link JsonObject} is backed by a {@link Map} and is intended to work similar to it API wise, but to only
 * allow to store such objects which can be represented by JSON.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class JsonObject extends JsonValue {

    /**
     * The backing {@link Map} for the object.
     */
    private final Map<String, Object> content;

    /**
     * Private constructor to create the object.
     */
    private JsonObject() {
        content = new HashMap<String, Object>();
    }

    /**
     * Creates a empty {@link JsonObject}.
     *
     * @return a empty {@link JsonObject}.
     */
    public static JsonObject empty() {
        return new JsonObject();
    }

    /**
     * Creates a empty {@link JsonObject}.
     *
     * @return a empty {@link JsonObject}.
     */
    public static JsonObject create() {
        return new JsonObject();
    }

    /**
     * Stores a {@link Object} value identified by the field name.
     *
     * Note that the value is checked and a {@link IllegalArgumentException} is thrown if not supported.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @return the {@link JsonObject}.
     */
    public JsonObject put(final String name, final Object value) {
        if (checkType(value)) {
            content.put(name, value);
        } else {
            throw new IllegalArgumentException("Unsupported type for JsonObject: " + value.getClass());
        }
        return this;
    }

    /**
     * Retrieves the (potential null) content and not casting its type.
     *
     * @param name the key of the field.
     * @return the value of the field, or null if it does not exist.
     */
    public Object get(final String name) {
        return content.get(name);
    }

    /**
     * Stores a {@link String} value identified by the field name.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @return the {@link JsonObject}.
     */
    public JsonObject put(final String name, final String value) {
        content.put(name, value);
        return this;
    }

    /**
     * Retrieves the value from the field name and casts it to {@link String}.
     *
     * @param name the name of the field.
     * @return the result or null if it does not exist.
     */
    public String getString(String name) {
        return (String) content.get(name);
    }

    /**
     * Stores a {@link Integer} value identified by the field name.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @return the {@link JsonObject}.
     */
    public JsonObject put(String name, int value) {
        content.put(name, value);
        return this;
    }

    /**
     * Retrieves the value from the field name and casts it to {@link Integer}.
     *
     * @param name the name of the field.
     * @return the result or null if it does not exist.
     */
    public Integer getInt(String name) {
        return (Integer) content.get(name);
    }

    /**
     * Stores a {@link Long} value identified by the field name.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @return the {@link JsonObject}.
     */
    public JsonObject put(String name, long value) {
        content.put(name, value);
        return this;
    }

    /**
     * Retrieves the value from the field name and casts it to {@link Long}.
     *
     * @param name the name of the field.
     * @return the result or null if it does not exist.
     */
    public Long getLong(String name) {
        return (Long) content.get(name);
    }

    /**
     * Stores a {@link Double} value identified by the field name.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @return the {@link JsonObject}.
     */
    public JsonObject put(String name, double value) {
        content.put(name, value);
        return this;
    }

    /**
     * Retrieves the value from the field name and casts it to {@link Double}.
     *
     * @param name the name of the field.
     * @return the result or null if it does not exist.
     */
    public Double getDouble(String name) {
        return (Double) content.get(name);
    }

    /**
     * Stores a {@link Boolean} value identified by the field name.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @return the {@link JsonObject}.
     */
    public JsonObject put(String name, boolean value) {
        content.put(name, value);
        return this;
    }

    /**
     * Retrieves the value from the field name and casts it to {@link Boolean}.
     *
     * @param name the name of the field.
     * @return the result or null if it does not exist.
     */
    public Boolean getBoolean(String name) {
        return (Boolean) content.get(name);
    }

    /**
     * Stores a {@link JsonObject} value identified by the field name.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @return the {@link JsonObject}.
     */
    public JsonObject put(String name, JsonObject value) {
        content.put(name, value);
        return this;
    }

    /**
     * Retrieves the value from the field name and casts it to {@link JsonObject}.
     *
     * @param name the name of the field.
     * @return the result or null if it does not exist.
     */
    public JsonObject getObject(String name) {
        return (JsonObject) content.get(name);
    }

    /**
     * Stores a {@link JsonArray} value identified by the field name.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @return the {@link JsonObject}.
     */
    public JsonObject put(String name, JsonArray value) {
        content.put(name, value);
        return this;
    }

    /**
     * Retrieves the value from the field name and casts it to {@link JsonArray}.
     *
     * @param name the name of the field.
     * @return the result or null if it does not exist.
     */
    public JsonArray getArray(String name) {
        return (JsonArray) content.get(name);
    }

    /**
     * Returns a set of field names on the {@link JsonObject}.
     *
     * @return the set of names on the object.
     */
    public Set<String> getNames() {
        return content.keySet();
    }

    /**
     * Returns true if the {@link JsonObject} is empty, false otherwise.
     *
     * @return true if empty, false otherwise.
     */
    public boolean isEmpty() {
        return content.isEmpty();
    }

    /**
     * Creates a copy of the underlying {@link Map} and returns it.
     *
     * @return the {@link Map} of the content.
     */
    public Map<String, Object> toMap() {
        return new HashMap<String, Object>(content);
    }

    /**
     * Checks if the {@link JsonObject} contains the field name.
     *
     * @param name the name of the field.
     * @return true if its contained, false otherwise.
     */
    public boolean containsKey(String name) {
        return content.containsKey(name);
    }

    /**
     * Checks if the {@link JsonObject} contains the value.
     *
     * @param value the actual value.
     * @return true if its contained, false otherwise.
     */
    public boolean containsValue(Object value) {
        return content.containsValue(value);
    }

    /**
     * The size of the {@link JsonObject}.
     *
     * @return the size.
     */
    public int size() {
        return content.size();
    }

    /**
     * Converts the {@link JsonObject} into its JSON string representation.
     *
     * @return the JSON string representing this {@link JsonObject}.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        int size = content.size();
        int item = 0;
        for(Map.Entry<String, Object> entry : content.entrySet()) {
            sb.append("\"").append(entry.getKey()).append("\":");
            if (entry.getValue() instanceof String) {
                sb.append("\"").append(entry.getValue()).append("\"");
            } else {
                if (entry.getValue() == null) {
                    sb.append("null");
                } else {
                    sb.append(entry.getValue().toString());
                }
            }
            if (++item < size) {
                sb.append(",");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JsonObject object = (JsonObject) o;

        if (content != null ? !content.equals(object.content) : object.content != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }
}
