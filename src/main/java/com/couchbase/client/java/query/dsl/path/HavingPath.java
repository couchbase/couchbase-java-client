package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Expression;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public interface HavingPath extends SelectResultPath {

    SelectResultPath having(Expression condition);

}
