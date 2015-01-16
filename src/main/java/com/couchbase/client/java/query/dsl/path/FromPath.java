package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Expression;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public interface FromPath extends LetPath {

    AsPath from(String from);

    /** Note that from Expression should be a single identifier/path **/
    AsPath from(Expression expression);

}
