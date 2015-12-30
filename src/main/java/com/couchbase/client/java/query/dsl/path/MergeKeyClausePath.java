package com.couchbase.client.java.query.dsl.path;


import com.couchbase.client.java.query.dsl.Expression;

public interface MergeKeyClausePath extends Path {

  MergeUpdatePath onKey(Expression expression);
  MergeUpdatePath onPrimaryKey(Expression expression);
  MergeUpdatePath onKey(String expression);
  MergeUpdatePath onPrimaryKey(String expression);


}
