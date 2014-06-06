package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.Query;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public interface OffsetPath extends Query, Path {

    Query offset(int offset);

}
