package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.MergeInsertElement;


public class DefaultMergeInsertPath extends DefaultMutateLimitPath implements MergeInsertPath {

  public DefaultMergeInsertPath(AbstractPath parent) {
    super(parent);
  }

  @Override
  public MergeInsertWherePath whenNotMatchedThenInsert(Expression expression) {
    element(new MergeInsertElement(expression));
    return new DefaultMergeInsertWherePath(this);
  }

}
