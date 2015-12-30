package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.WhereElement;

import static com.couchbase.client.java.query.dsl.Expression.x;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DefaultMergeDeleteWherePath extends DefaultMergeInsertPath implements MergeDeleteWherePath {

  public DefaultMergeDeleteWherePath(AbstractPath parent) {
    super(parent);
  }

  @Override
  public MergeInsertPath where(Expression expression) {
    element(new WhereElement(expression));
    return new DefaultMergeInsertPath(this);
  }

  @Override
  public MergeInsertPath where(String expression) {
    return where(x(expression));
  }
}
