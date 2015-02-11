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

import com.couchbase.client.deps.com.fasterxml.jackson.core.JsonProcessingException;
import com.couchbase.client.java.CouchbaseAsyncBucket;
import com.couchbase.client.java.transcoder.JacksonTransformers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
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
 * @author Simon Baslé
 * @since 2.0
 */
public class JsonObject extends JsonValue implements Serializable {

    private static final long serialVersionUID = 8817717605659870262L;

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
     * Constructs a {@link JsonObject} from a {@link Map Map&lt;String, ?&gt;}.
     *
     * This is only possible if the given Map is well formed, that is it contains non null
     * keys, and all values are of a supported type.
     *
     * A null input Map or null key will lead to a {@link NullPointerException} being thrown.
     * If any unsupported value is present in the Map, an {@link IllegalArgumentException}
     * will be thrown.
     *
     * *Sub Maps and Lists*
     * If possible, Maps and Lists contained in mapData will be converted to JsonObject and
     * JsonArray respectively. However, same restrictions apply. Any non-convertible collection
     * will raise a {@link ClassCastException}. If the sub-conversion raises an exception (like an
     * IllegalArgumentException) then it is put as cause for the ClassCastException.
     *
     * @param mapData the Map to convert to a JsonObject
     * @return the resulting JsonObject
     * @throws IllegalArgumentException in case one or more unsupported values are present
     * @throws NullPointerException in case a null map is provided or if it contains a null key
     * @throws ClassCastException if map contains a sub-Map or sub-List not supported (see above)
     */
    public static JsonObject from(Map<String, ?> mapData) {
        if (mapData == null) {
            throw new NullPointerException("Null input Map unsupported");
        } else if (mapData.isEmpty()) {
            return JsonObject.empty();
        }

        JsonObject result = new JsonObject();
        for (Map.Entry<String, ?> entry : mapData.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value == JsonValue.NULL) {
                value = null;
            }

            if (key == null) {
                throw new NullPointerException("The key is not allowed to be null");
            } else if (value instanceof Map) {
                try {
                    JsonObject sub = JsonObject.from((Map<String, ?>) value);
                    result.put(key, sub);
                } catch (ClassCastException e) {
                    throw e;
                } catch (Exception e) {
                    ClassCastException c = new ClassCastException("Couldn't convert sub-Map " + key + " to JsonObject");
                    c.initCause(e);
                    throw c;
                }
            } else if (value instanceof List) {
                try {
                    JsonArray sub = JsonArray.from((List<?>) value);
                    result.put(key, sub);
                } catch (Exception e) {
                    //no risk of a direct ClassCastException here
                    ClassCastException c = new ClassCastException("Couldn't convert sub-List " + key + " to JsonArray");
                    c.initCause(e);
                    throw c;
                }
            } else if (!checkType(value)) {
                throw new IllegalArgumentException("Unsupported type for JsonObject: " + value.getClass());
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * Static method to create a {@link JsonObject} from a JSON {@link String}.
     *
     * The string is expected to be a valid JSON object representation (eg. starting with a '{').
     *
     * @param s the JSON String to convert to a {@link JsonObject}.
     * @return the corresponding {@link JsonObject}.
     * @throws IllegalArgumentException if the conversion cannot be done.
     */
    public static JsonObject fromJson(String s) {
        try {
            return CouchbaseAsyncBucket.JSON_OBJECT_TRANSCODER.stringToJsonObject(s);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot convert string to JsonObject", e);
        }
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
        if (this == value) {
            throw new IllegalArgumentException("Cannot put self");
        } else if (value == JsonValue.NULL) {
            putNull(name);
        } else if (checkType(value)) {
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
     * Note that if value was stored as another numerical type, some truncation or rounding may occur.
     *
     * @param name the name of the field.
     * @return the result or null if it does not exist.
     */
    public Integer getInt(String name) {
        //let it fail in the more general case where it isn't actually a number
        Number number = (Number) content.get(name);
        if (number == null) {
            return null;
        } else if (number instanceof Integer) {
            return (Integer) number;
        } else {
            return number.intValue(); //autoboxing to Integer
        }
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
     * Note that if value was stored as another numerical type, some truncation or rounding may occur.
     *
     * @param name the name of the field.
     * @return the result or null if it does not exist.
     */
    public Long getLong(String name) {
        //let it fail in the more general case where it isn't actually a number
        Number number = (Number) content.get(name);
        if (number == null) {
            return null;
        } else if (number instanceof Long) {
            return (Long) number;
        } else {
            return number.longValue(); //autoboxing to Long
        }
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
     * Note that if value was stored as another numerical type, some truncation or rounding may occur.
     *
     * @param name the name of the field.
     * @return the result or null if it does not exist.
     */
    public Double getDouble(String name) {
        //let it fail in the more general case where it isn't actually a number
        Number number = (Number) content.get(name);
        if (number == null) {
            return null;
        } else if (number instanceof Double) {
            return (Double) number;
        } else {
            return number.doubleValue(); //autoboxing to Double
        }
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
        if (this == value) {
            throw new IllegalArgumentException("Cannot put self");
        }
        content.put(name, value);
        return this;
    }

    /**
     * Attempt to convert a {@link Map} to a {@link JsonObject} value and store it, identified by the field name.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @return the {@link JsonObject}.
     * @see #from(Map)
     */
    public JsonObject put(String name, Map<String, ?> value) {
        return put(name, JsonObject.from(value));
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
     * Stores a {@link JsonArray} value identified by the field name.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @return the {@link JsonObject}.
     */
    public JsonObject put(String name, List<?> value) {
        return put(name, JsonArray.from(value));
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
     * Store a null value identified by the field's name.
     *
     * This method is equivalent to calling {@link #put(String, Object)} with either
     * {@link JsonValue#NULL JsonValue.NULL} or a null value explicitly cast to Object.
     *
     * @param name The null field's name.
     * @return the {@link JsonObject}
     */
    public JsonObject putNull(String name) {
        content.put(name, null);
        return this;
    }

    /**
     * Removes an entry from the {@link JsonObject}.
     *
     * @param name the name of the field to remove
     * @return the {@link JsonObject}
     */
    public JsonObject removeKey(String name) {
        content.remove(name);
        return this;
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
     * Transforms the {@link JsonObject} into a {@link Map}. The resulting
     * map is not backed by this {@link JsonObject}, and all sub-objects or
     * sub-arrays ({@link JsonArray}) are also recursively converted to
     * maps and lists, respectively.
     *
     * @return the content copied as a {@link Map}.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> copy = new HashMap<String, Object>(content.size());
        for (Map.Entry<String, Object> entry : content.entrySet()) {
            Object content = entry.getValue();
            if (content instanceof JsonObject) {
                copy.put(entry.getKey(), ((JsonObject) content).toMap());
            } else if (content instanceof JsonArray) {
                copy.put(entry.getKey(), ((JsonArray) content).toList());
            } else {
                copy.put(entry.getKey(), content);
            }
        }
        return copy;
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
        try {
            return JacksonTransformers.MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot convert JsonObject to Json String", e);
        }
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
