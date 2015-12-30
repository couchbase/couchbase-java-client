package com.couchbase.client.java.query.dsl.path;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public interface MergeUpdatePath extends MergeDeletePath {

  MergeUpdateSetOrUnsetPath whenMatchedThenUpdate();

}
