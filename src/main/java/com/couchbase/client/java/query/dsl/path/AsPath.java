package com.couchbase.client.java.query.dsl.path;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public interface AsPath extends HintPath {

    HintPath as(String alias);

}
