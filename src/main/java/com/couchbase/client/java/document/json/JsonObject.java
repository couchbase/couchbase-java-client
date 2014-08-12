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
 * Represents a JSON Object, which acts very much like a java Map, but is limited
 * encode the types supported in JSON.
 */
public class JsonObject implements JsonValue {

  /**
   * The map which backs the object itself.
   */
  private final Map<String, Object> content;

  /**
   * Creates a new {@link JsonObject}.
   */
  private JsonObject() {
    content = new HashMap<String, Object>();
  }

  /**
   * Factory method encode empty a new and empty {@link JsonObject}.
   * @return a new {@link JsonObject}.
   */
  public static JsonObject empty() {
    return new JsonObject();
  }

  public Object get(String name) {
    return content.get(name);
  }

  public JsonObject put(String name, String value) {
    content.put(name, value);
    return this;
  }

  public String getString(String name) {
    return (String) content.get(name);
  }

  public JsonObject put(String name, int value) {
    content.put(name, value);
    return this;
  }

  public Integer getInt(String name) {
    return (Integer) content.get(name);
  }

  public JsonObject put(String name, long value) {
    content.put(name, value);
    return this;
  }

  public Long getLong(String name) {
      return (Long) content.get(name);
  }

  public JsonObject put(String name, double value) {
    content.put(name, value);
    return this;
  }

  public Double getDouble(String name) {
    return (Double) content.get(name);
  }

  public JsonObject put(String name, boolean value) {
    content.put(name, value);
    return this;
  }

  public Boolean getBoolean(String name) {
    return (Boolean) content.get(name);
  }

  public JsonObject put(String name, JsonObject value) {
    content.put(name, value);
    return this;
  }

  public JsonObject getObject(String name) {
    return (JsonObject) content.get(name);
  }

  public JsonObject put(String name, JsonArray value) {
    content.put(name, value);
    return this;
  }

  public JsonArray getArray(String name) {
    return (JsonArray) content.get(name);
  }

  public Set<String> getNames() {
    return content.keySet();
  }

  public boolean isEmpty() {
    return content.isEmpty();
  }

  public Map<String, Object> toMap() {
    return new HashMap<String, Object>(content);
  }

  public boolean containsKey(String name) {
     return content.containsKey(name);
  }

  public boolean containsValue(Object value) {
      return content.containsValue(value);
  }

  public int size() {
    return content.size();
  }

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
}
