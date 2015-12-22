package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.SetElement;

import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;

public class DefaultUpdateSetPath extends DefaultInitialUpdateUnsetPath implements UpdateSetPath {

  private static final SetElement.SetPosition POS = SetElement.SetPosition.NOT_INITIAL;

  public DefaultUpdateSetPath(AbstractPath parent) {
    super(parent);
  }

  @Override
  public UpdateSetPath set(String path, Expression value) {
    element(new SetElement(POS, x(path), value, null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, JsonObject value) {
    element(new SetElement(POS, x(path), x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, JsonArray value) {
    element(new SetElement(POS, x(path), x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, String value) {
    element(new SetElement(POS, x(path), s(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, int value) {
    element(new SetElement(POS, x(path), x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, long value) {
    element(new SetElement(POS, x(path), x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, double value) {
    element(new SetElement(POS, x(path), x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, float value) {
    element(new SetElement(POS, x(path), x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, boolean value) {
    element(new SetElement(POS, x(path), x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, Expression value, Expression updateFor) {
    element(new SetElement(POS, x(path), value, updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, JsonObject value, Expression updateFor) {
    element(new SetElement(POS, x(path), x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, JsonArray value, Expression updateFor) {
    element(new SetElement(POS, x(path), x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, String value, Expression updateFor) {
    element(new SetElement(POS, x(path), s(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, int value, Expression updateFor) {
    element(new SetElement(POS, x(path), x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, long value, Expression updateFor) {
    element(new SetElement(POS, x(path), x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, double value, Expression updateFor) {
    element(new SetElement(POS, x(path), x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, float value, Expression updateFor) {
    element(new SetElement(POS, x(path), x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(String path, boolean value, Expression updateFor) {
    element(new SetElement(POS, x(path), x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, Expression value) {
    element(new SetElement(POS, path, value, null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, JsonObject value) {
    element(new SetElement(POS, path, x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, JsonArray value) {
    element(new SetElement(POS, path, x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, String value) {
    element(new SetElement(POS, path, s(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, int value) {
    element(new SetElement(POS, path, x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, long value) {
    element(new SetElement(POS, path, x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, double value) {
    element(new SetElement(POS, path, x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, float value) {
    element(new SetElement(POS, path, x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, boolean value) {
    element(new SetElement(POS, path, x(value), null));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, Expression value, Expression updateFor) {
    element(new SetElement(POS, path, value, updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, JsonObject value, Expression updateFor) {
    element(new SetElement(POS, path, x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, JsonArray value, Expression updateFor) {
    element(new SetElement(POS, path, x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, String value, Expression updateFor) {
    element(new SetElement(POS, path, s(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, int value, Expression updateFor) {
    element(new SetElement(POS, path, x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, long value, Expression updateFor) {
    element(new SetElement(POS, path, x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, double value, Expression updateFor) {
    element(new SetElement(POS, path, x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, float value, Expression updateFor) {
    element(new SetElement(POS, path, x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }

  @Override
  public UpdateSetPath set(Expression path, boolean value, Expression updateFor) {
    element(new SetElement(POS, path, x(value), updateFor));
    return new DefaultUpdateSetPath(this);
  }
}
