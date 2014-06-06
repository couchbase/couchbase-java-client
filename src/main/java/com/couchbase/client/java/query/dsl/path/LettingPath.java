package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Alias;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public interface LettingPath extends HavingPath {

    HavingPath letting(Alias... aliases);

}
