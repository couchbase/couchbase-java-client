package com.couchbase.client.java.query.dsl.path;


import com.couchbase.client.java.query.dsl.Expression;

public interface MergeUpdateUnsetPath extends MergeUpdateWherePath {

  MergeUpdateUnsetPath unset(String path);
  MergeUpdateUnsetPath unset(String path, Expression updateFor);
  MergeUpdateUnsetPath unset(Expression path);
  MergeUpdateUnsetPath unset(Expression path, Expression updateFor);

}
