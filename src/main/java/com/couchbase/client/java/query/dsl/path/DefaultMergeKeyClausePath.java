package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.Element;

import static com.couchbase.client.java.query.dsl.Expression.x;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DefaultMergeKeyClausePath extends AbstractPath implements MergeKeyClausePath {

  public DefaultMergeKeyClausePath(AbstractPath parent) {
    super(parent);
  }

  @Override
  public MergeUpdatePath onKey(final Expression expression) {
    element(new Element() {
      @Override
      public String export() {
        return "ON KEY " + expression.toString();
      }
    });
    return new DefaultMergeUpdatePath(this);
  }

  @Override
  public MergeUpdatePath onPrimaryKey(final Expression expression) {
    element(new Element() {
      @Override
      public String export() {
        return "ON PRIMARY KEY " + expression.toString();
      }
    });
    return new DefaultMergeUpdatePath(this);
  }

  @Override
  public MergeUpdatePath onKey(String expression) {
    return onKey(x(expression));
  }

  @Override
  public MergeUpdatePath onPrimaryKey(String expression) {
    return onPrimaryKey(x(expression));
  }
}
