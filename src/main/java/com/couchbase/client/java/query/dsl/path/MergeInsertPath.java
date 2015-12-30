package com.couchbase.client.java.query.dsl.path;


import com.couchbase.client.java.query.dsl.Expression;

public interface MergeInsertPath extends MutateLimitPath {

  MergeInsertWherePath whenNotMatchedThenInsert(Expression expression);

}
