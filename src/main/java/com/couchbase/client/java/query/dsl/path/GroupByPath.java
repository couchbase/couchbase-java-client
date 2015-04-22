package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.query.dsl.Expression;

/**
 * .
 *
 * @author Michael Nitschinger
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public interface GroupByPath extends SelectResultPath {

    LettingPath groupBy(Expression... expressions);

    LettingPath groupBy(String... identifiers);

}
