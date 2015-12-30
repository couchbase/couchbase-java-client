package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.MergeDeleteElement;


public class DefaultMergeDeletePath extends DefaultMergeInsertPath implements MergeDeletePath {

  public DefaultMergeDeletePath(AbstractPath parent) {
    super(parent);
  }


  @Override
  public MergeDeleteWherePath whenMatchedThenDelete() {
    element(new MergeDeleteElement());
    return new DefaultMergeDeleteWherePath(this);
  }
}
