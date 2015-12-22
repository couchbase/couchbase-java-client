package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.UnsetElement;

import static com.couchbase.client.java.query.dsl.Expression.x;

public class DefaultUpdateUnsetPath extends DefaultMutateWherePath implements UpdateUnsetPath {

  public DefaultUpdateUnsetPath(AbstractPath parent) {
    super(parent);
  }

  @Override
  public UpdateUnsetPath unset(String path) {
    element(new UnsetElement(UnsetElement.UnsetPosition.NOT_INITIAL, x(path), null));
    return new DefaultUpdateUnsetPath(this);
  }

  @Override
  public UpdateUnsetPath unset(String path, Expression updateFor) {
    element(new UnsetElement(UnsetElement.UnsetPosition.NOT_INITIAL, x(path), updateFor));
    return new DefaultUpdateUnsetPath(this);
  }

  @Override
  public UpdateUnsetPath unset(Expression path) {
    element(new UnsetElement(UnsetElement.UnsetPosition.NOT_INITIAL, path, null));
    return new DefaultUpdateUnsetPath(this);
  }

  @Override
  public UpdateUnsetPath unset(Expression path, Expression updateFor) {
    element(new UnsetElement(UnsetElement.UnsetPosition.NOT_INITIAL, path, updateFor));
    return new DefaultUpdateUnsetPath(this);
  }
}
