package com.couchbase.client.java.query.dsl.path;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public interface JoinPath extends KeysPath {

    KeysPath as(String alias);

}
