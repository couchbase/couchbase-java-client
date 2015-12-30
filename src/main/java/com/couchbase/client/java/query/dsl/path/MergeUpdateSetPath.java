package com.couchbase.client.java.query.dsl.path;


import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.dsl.Expression;

public interface MergeUpdateSetPath extends MergeUpdateWherePath {

  MergeUpdateUnsetPath unset(String path);
  MergeUpdateUnsetPath unset(String path, Expression updateFor);
  MergeUpdateUnsetPath unset(Expression path);
  MergeUpdateUnsetPath unset(Expression path, Expression updateFor);

  MergeUpdateSetPath set(String path, Expression value);
  MergeUpdateSetPath set(String path, JsonObject value);
  MergeUpdateSetPath set(String path, JsonArray value);
  MergeUpdateSetPath set(String path, String value);
  MergeUpdateSetPath set(String path, int value);
  MergeUpdateSetPath set(String path, long value);
  MergeUpdateSetPath set(String path, double value);
  MergeUpdateSetPath set(String path, float value);
  MergeUpdateSetPath set(String path, boolean value);

  MergeUpdateSetPath set(String path, Expression value, Expression updateFor);
  MergeUpdateSetPath set(String path, JsonObject value, Expression updateFor);
  MergeUpdateSetPath set(String path, JsonArray value, Expression updateFor);
  MergeUpdateSetPath set(String path, String value, Expression updateFor);
  MergeUpdateSetPath set(String path, int value, Expression updateFor);
  MergeUpdateSetPath set(String path, long value, Expression updateFor);
  MergeUpdateSetPath set(String path, double value, Expression updateFor);
  MergeUpdateSetPath set(String path, float value, Expression updateFor);
  MergeUpdateSetPath set(String path, boolean value, Expression updateFor);

  MergeUpdateSetPath set(Expression path, Expression value);
  MergeUpdateSetPath set(Expression path, JsonObject value);
  MergeUpdateSetPath set(Expression path, JsonArray value);
  MergeUpdateSetPath set(Expression path, String value);
  MergeUpdateSetPath set(Expression path, int value);
  MergeUpdateSetPath set(Expression path, long value);
  MergeUpdateSetPath set(Expression path, double value);
  MergeUpdateSetPath set(Expression path, float value);
  MergeUpdateSetPath set(Expression path, boolean value);

  MergeUpdateSetPath set(Expression path, Expression value, Expression updateFor);
  MergeUpdateSetPath set(Expression path, JsonObject value, Expression updateFor);
  MergeUpdateSetPath set(Expression path, JsonArray value, Expression updateFor);
  MergeUpdateSetPath set(Expression path, String value, Expression updateFor);
  MergeUpdateSetPath set(Expression path, int value, Expression updateFor);
  MergeUpdateSetPath set(Expression path, long value, Expression updateFor);
  MergeUpdateSetPath set(Expression path, double value, Expression updateFor);
  MergeUpdateSetPath set(Expression path, float value, Expression updateFor);
  MergeUpdateSetPath set(Expression path, boolean value, Expression updateFor);

}
