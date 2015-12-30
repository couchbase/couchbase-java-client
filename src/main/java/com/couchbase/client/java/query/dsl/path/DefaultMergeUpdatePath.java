package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.element.Element;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DefaultMergeUpdatePath extends DefaultMergeDeletePath implements MergeUpdatePath {

  public DefaultMergeUpdatePath(AbstractPath parent) {
    super(parent);
  }

  @Override
  public MergeUpdateSetOrUnsetPath whenMatchedThenUpdate() {
    element(new Element() {
      @Override
      public String export() {
        return "WHEN MATCHED THEN UPDATE";
      }
    });
    return new DefaultMergeUpdateSetOrUnsetPath(this);
  }
}
