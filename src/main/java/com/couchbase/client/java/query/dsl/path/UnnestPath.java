package com.couchbase.client.java.query.dsl.path;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public interface UnnestPath extends LetPath {

    LetPath as(String alias);

}
