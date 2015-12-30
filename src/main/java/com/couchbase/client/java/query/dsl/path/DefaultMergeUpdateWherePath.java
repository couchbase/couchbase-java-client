package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.WhereElement;

import static com.couchbase.client.java.query.dsl.Expression.x;

public class DefaultMergeUpdateWherePath extends DefaultMergeDeletePath implements MergeUpdateWherePath {

  public DefaultMergeUpdateWherePath(AbstractPath parent) {
    super(parent);
  }

  @Override
  public MergeDeletePath where(Expression expression) {
    element(new WhereElement(expression));
    return new DefaultMergeDeletePath(this);
  }

  @Override
  public MergeDeletePath where(String expression) {
    return where(x(expression));
  }
}
