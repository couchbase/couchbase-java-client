package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Sort;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public interface OrderByPath extends LimitPath {

    LimitPath orderBy(Sort... orderings);

}
