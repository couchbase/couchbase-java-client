package com.couchbase.client.java.query.dsl.path;


import com.couchbase.client.java.query.dsl.Expression;

public interface MergeDeleteWherePath extends MergeInsertPath {

  MergeInsertPath where(Expression expression);
  MergeInsertPath where(String expression);

}
