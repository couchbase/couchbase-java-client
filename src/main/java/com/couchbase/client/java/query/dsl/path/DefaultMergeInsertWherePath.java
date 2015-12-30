package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.WhereElement;

import static com.couchbase.client.java.query.dsl.Expression.x;


public class DefaultMergeInsertWherePath extends DefaultMutateLimitPath implements MergeInsertWherePath {

  public DefaultMergeInsertWherePath(AbstractPath parent) {
    super(parent);
  }

  @Override
  public MutateLimitPath where(Expression expression) {
    element(new WhereElement(expression));
    return new DefaultMutateLimitPath(this);
  }

  @Override
  public MutateLimitPath where(String expression) {
    return where(x(expression));
  }
}
