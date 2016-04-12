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
import com.couchbase.client.java.query.dsl.element.SetElement;
import com.couchbase.client.java.query.dsl.element.UnsetElement;

import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;

public class DefaultMergeUpdateSetPath extends DefaultMergeUpdateWherePath implements MergeUpdateSetPath {

  private static final SetElement.SetPosition POS = SetElement.SetPosition.NOT_INITIAL;

  public DefaultMergeUpdateSetPath(AbstractPath parent) {
    super(parent);
  }

  @Override
  public MergeUpdateUnsetPath unset(String path) {
    return unset(x(path), null);
  }

  @Override
  public MergeUpdateUnsetPath unset(String path, Expression updateFor) {
    return unset(x(path), updateFor);
  }

  @Override
  public MergeUpdateUnsetPath unset(Expression path) {
    return unset(path, null);
  }

  @Override
  public MergeUpdateUnsetPath unset(Expression path, Expression updateFor) {
    element(new UnsetElement(UnsetElement.UnsetPosition.INITIAL, path, updateFor));
    return new DefaultMergeUpdateUnsetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(String path, Expression value) {
    element(new SetElement(POS, x(path), value, null));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(String path, JsonObject value) {
    element(new SetElement(POS, x(path), x(value), null));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(String path, JsonArray value) {
    element(new SetElement(POS, x(path), x(value), null));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(String path, String value) {
    element(new SetElement(POS, x(path), s(value), null));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(String path, int value) {
    element(new SetElement(POS, x(path), x(value), null));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(String path, long value) {
    element(new SetElement(POS, x(path), x(value), null));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(String path, double value) {
    element(new SetElement(POS, x(path), x(value), null));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(String path, float value) {
    element(new SetElement(POS, x(path), x(value), null));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(String path, boolean value) {
    element(new SetElement(POS, x(path), x(value), null));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(String path, Expression value, Expression updateFor) {
    element(new SetElement(POS, x(path), value, updateFor));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(String path, JsonObject value, Expression updateFor) {
    element(new SetElement(POS, x(path), x(value), updateFor));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(String path, JsonArray value, Expression updateFor) {
    element(new SetElement(POS, x(path), x(value), updateFor));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(String path, String value, Expression updateFor) {
    element(new SetElement(POS, x(path), s(value), updateFor));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(String path, int value, Expression updateFor) {
    element(new SetElement(POS, x(path), x(value), updateFor));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(String path, long value, Expression updateFor) {
    element(new SetElement(POS, x(path), x(value), updateFor));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(String path, double value, Expression updateFor) {
    element(new SetElement(POS, x(path), x(value), updateFor));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(String path, float value, Expression updateFor) {
    element(new SetElement(POS, x(path), x(value), updateFor));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(String path, boolean value, Expression updateFor) {
    element(new SetElement(POS, x(path), x(value), updateFor));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(Expression path, Expression value) {
    element(new SetElement(POS, path, value, null));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(Expression path, JsonObject value) {
    element(new SetElement(POS, path, x(value), null));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(Expression path, JsonArray value) {
    element(new SetElement(POS, path, x(value), null));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(Expression path, String value) {
    element(new SetElement(POS, path, s(value), null));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(Expression path, int value) {
    element(new SetElement(POS, path, x(value), null));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(Expression path, long value) {
    element(new SetElement(POS, path, x(value), null));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(Expression path, double value) {
    element(new SetElement(POS, path, x(value), null));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(Expression path, float value) {
    element(new SetElement(POS, path, x(value), null));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(Expression path, boolean value) {
    element(new SetElement(POS, path, x(value), null));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(Expression path, Expression value, Expression updateFor) {
    element(new SetElement(POS, path, value, updateFor));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(Expression path, JsonObject value, Expression updateFor) {
    element(new SetElement(POS, path, x(value), updateFor));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(Expression path, JsonArray value, Expression updateFor) {
    element(new SetElement(POS, path, x(value), updateFor));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(Expression path, String value, Expression updateFor) {
    element(new SetElement(POS, path, s(value), updateFor));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(Expression path, int value, Expression updateFor) {
    element(new SetElement(POS, path, x(value), updateFor));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(Expression path, long value, Expression updateFor) {
    element(new SetElement(POS, path, x(value), updateFor));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(Expression path, double value, Expression updateFor) {
    element(new SetElement(POS, path, x(value), updateFor));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(Expression path, float value, Expression updateFor) {
    element(new SetElement(POS, path, x(value), updateFor));
    return new DefaultMergeUpdateSetPath(this);
  }

  @Override
  public MergeUpdateSetPath set(Expression path, boolean value, Expression updateFor) {
    element(new SetElement(POS, path, x(value), updateFor));
    return new DefaultMergeUpdateSetPath(this);
  }

}
