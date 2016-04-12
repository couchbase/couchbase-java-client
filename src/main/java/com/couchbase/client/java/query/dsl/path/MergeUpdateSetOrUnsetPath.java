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
package com.couchbase.client.java.query.dsl.path;


import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.dsl.Expression;

public interface MergeUpdateSetOrUnsetPath extends MergeUpdateWherePath {

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
