package com.couchbase.client.java.document.json;

import java.util.ArrayList;
import java.util.List;

public class JsonArray implements JsonValue {

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
        return (Long) content.get(index);
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
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < content.size(); i++) {
            Object item = content.get(i);
            boolean isString = item instanceof String;

            if (isString) {
                sb.append("\"");
            }
            sb.append(item.toString());
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
