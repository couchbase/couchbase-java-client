package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Expression;

public interface InitialUpdateUnsetPath extends MutateWherePath {

  UpdateUnsetPath unset(String path);
  UpdateUnsetPath unset(String path, Expression updateFor);
  UpdateUnsetPath unset(Expression path);
  UpdateUnsetPath unset(Expression path, Expression updateFor);

}
