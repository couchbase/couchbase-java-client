package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Expression;

public interface MergeSourcePath extends Path {

  MergeKeyClausePath using(String source);
  MergeKeyClausePath using(Expression source);

}
