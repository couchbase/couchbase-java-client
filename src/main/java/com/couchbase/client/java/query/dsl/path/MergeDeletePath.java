package com.couchbase.client.java.query.dsl.path;


public interface MergeDeletePath extends MergeInsertPath {

  MergeDeleteWherePath whenMatchedThenDelete();

}
