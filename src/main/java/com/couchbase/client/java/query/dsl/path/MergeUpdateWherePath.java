package com.couchbase.client.java.query.dsl.path;


import com.couchbase.client.java.query.dsl.Expression;

public interface MergeUpdateWherePath extends MergeDeletePath {

  MergeDeletePath where(Expression expression);
  MergeDeletePath where(String expression);

}
