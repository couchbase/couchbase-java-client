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

public class JsonArray implements JsonValue, Iterable<Object> {

      private final List<Object> content;

      private JsonArray() {
        content = new ArrayList<Object>();
      }

      public static JsonArray empty() {
        return new JsonArray();
      }

    public static JsonArray from(Object... items) {
        JsonArray array = new JsonArray();
        for (Object item : items) {
            array.add(item);
        }
        return array;
    }

      public Object get(int index) {
        return content.get(index);
      }

      public JsonArray add(String value) {
        content.add(value);
        return this;
      }

      public String getString(int index) {
        return (String) content.get(index);
      }

      public JsonArray add(long value) {
        content.add(value);
        return this;
      }

      public long getLong(int index) {
          Object found = content.get(index);
          if (found == null) {
              throw new NullPointerException();
          }
          if (found instanceof Integer) {
              return (Integer) found;
          }
          return (Long) found;
      }

      public JsonArray add(int value) {
        content.add(value);
        return this;
      }

      public int getInt(int index) {
        return (Integer) content.get(index);
      }

      public JsonArray add(double value) {
        content.add(value);
        return this;
      }

      public double getDouble(int index) {
        return (Double) content.get(index);
      }

      public JsonArray add(boolean value) {
        content.add(value);
        return this;
      }

      public boolean getBoolean(int index) {
        return (Boolean) content.get(index);
      }

      public JsonArray add(JsonObject value) {
        content.add(value);
        return this;
      }

      public JsonObject getObject(int index) {
        return (JsonObject) content.get(index);
      }

      public JsonArray add(JsonArray value) {
        content.add(value);
        return this;
      }

      public JsonArray add(Object value) {
          content.add(value);
          return this;
      }

      public JsonArray getArray(int index) {
        return (JsonArray) content.get(index);
      }

      public List<Object> toList() {
        return content;
      }

      public boolean isEmpty() {
        return content.isEmpty();
      }

      public int size() {
        return content.size();
      }

    @Override
    public Iterator<Object> iterator() {
        return content.iterator();
    }

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
}
