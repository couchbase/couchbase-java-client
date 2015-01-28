package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.Statement;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public interface OffsetPath extends Statement, Path {

    Statement offset(int offset);

}
