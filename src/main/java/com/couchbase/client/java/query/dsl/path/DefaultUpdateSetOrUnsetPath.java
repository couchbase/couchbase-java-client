package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.SetElement;
import com.couchbase.client.java.query.dsl.element.UnsetElement;

import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;

public class DefaultUpdateSetOrUnsetPath extends DefaultMutateWherePath
  implements UpdateSetOrUnsetPath {

  private static final UnsetElement.UnsetPosition UNSET_POS = UnsetElement.UnsetPosition.INITIAL;
  private static final SetElement.SetPosition SET_POS = SetElement.SetPosition.INITIAL;

  public DefaultUpdateSetOrUnsetPath(AbstractPath parent) {
    super(parent);
  }

  @Override
  public UpdateUnsetPath unset(String path) {
    element(new UnsetElement(UNSET_POS, x(path), null));
    return new DefaultUpdateUnsetPath(this);
  }

  @Override
  public UpdateUnsetPath unset(String path, Expression updateFor) {
    element(new UnsetElement(UNSET_POS, x(path), updateFor));
    return new DefaultUpdateUnsetPath(this);
  }

  @Override
  public UpdateUnsetPath unset(Expression path) {
    element(new UnsetElement(UNSET_POS, path, null));
    return new DefaultUpdateUnsetPath(this);
  }

  @Override
  public UpdateUnsetPath unset(Expression path, Expression updateFor) {
    element(new UnsetElement(UNSET_POS, path, updateFor));
    return new DefaultUpdateUnsetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, Expression value) {
    element(new SetElement(SET_POS, x(path), value, null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, JsonObject value) {
    element(new SetElement(SET_POS, x(path), x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, JsonArray value) {
    element(new SetElement(SET_POS, x(path), x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, String value) {
    element(new SetElement(SET_POS, x(path), s(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, int value) {
    element(new SetElement(SET_POS, x(path), x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, long value) {
    element(new SetElement(SET_POS, x(path), x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, double value) {
    element(new SetElement(SET_POS, x(path), x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, float value) {
    element(new SetElement(SET_POS, x(path), x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, boolean value) {
    element(new SetElement(SET_POS, x(path), x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, Expression value, Expression updateFor) {
    element(new SetElement(SET_POS, x(path), value, updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, JsonObject value, Expression updateFor) {
    element(new SetElement(SET_POS, x(path), x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, JsonArray value, Expression updateFor) {
    element(new SetElement(SET_POS, x(path), x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, String value, Expression updateFor) {
    element(new SetElement(SET_POS, x(path), s(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, int value, Expression updateFor) {
    element(new SetElement(SET_POS, x(path), x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, long value, Expression updateFor) {
    element(new SetElement(SET_POS, x(path), x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, double value, Expression updateFor) {
    element(new SetElement(SET_POS, x(path), x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, float value, Expression updateFor) {
    element(new SetElement(SET_POS, x(path), x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, boolean value, Expression updateFor) {
    element(new SetElement(SET_POS, x(path), x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, Expression value) {
    element(new SetElement(SET_POS, path, value, null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, JsonObject value) {
    element(new SetElement(SET_POS, path, x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, JsonArray value) {
    element(new SetElement(SET_POS, path, x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, String value) {
    element(new SetElement(SET_POS, path, s(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, int value) {
    element(new SetElement(SET_POS, path, x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, long value) {
    element(new SetElement(SET_POS, path, x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, double value) {
    element(new SetElement(SET_POS, path, x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, float value) {
    element(new SetElement(SET_POS, path, x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, boolean value) {
    element(new SetElement(SET_POS, path, x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, Expression value, Expression updateFor) {
    element(new SetElement(SET_POS, path, value, updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, JsonObject value, Expression updateFor) {
    element(new SetElement(SET_POS, path, x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, JsonArray value, Expression updateFor) {
    element(new SetElement(SET_POS, path, x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, String value, Expression updateFor) {
    element(new SetElement(SET_POS, path, s(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, int value, Expression updateFor) {
    element(new SetElement(SET_POS, path, x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, long value, Expression updateFor) {
    element(new SetElement(SET_POS, path, x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, double value, Expression updateFor) {
    element(new SetElement(SET_POS, path, x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, float value, Expression updateFor) {
    element(new SetElement(SET_POS, path, x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, boolean value, Expression updateFor) {
    element(new SetElement(SET_POS, path, x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }
}
