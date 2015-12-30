package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.UnsetElement;

import static com.couchbase.client.java.query.dsl.Expression.x;

public class DefaultMergeUpdateUnsetPath extends DefaultMergeUpdateWherePath implements MergeUpdateUnsetPath {

  public DefaultMergeUpdateUnsetPath(AbstractPath parent) {
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
    element(new UnsetElement(UnsetElement.UnsetPosition.NOT_INITIAL, path, updateFor));
    return new DefaultMergeUpdateUnsetPath(this);
  }

}
