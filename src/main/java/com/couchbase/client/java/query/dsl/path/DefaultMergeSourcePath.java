package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.Element;

import static com.couchbase.client.java.query.dsl.Expression.x;

public class DefaultMergeSourcePath extends AbstractPath implements MergeSourcePath {

  public DefaultMergeSourcePath(AbstractPath parent) {
    super(parent);
  }

  @Override
  public MergeKeyClausePath using(String source) {
    return using(x(source));
  }

  @Override
  public MergeKeyClausePath using(final Expression source) {
    element(new Element() {
      @Override
      public String export() {
        return "USING " + source.toString();
      }
    });
    return new DefaultMergeKeyClausePath(this);
  }

}
