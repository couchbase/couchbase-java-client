package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Expression;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public interface SelectPath extends Path {

    FromPath select(Expression... expressions);

    FromPath select(String... expressions);

    FromPath selectAll(Expression... expressions);

    FromPath selectAll(String... expressions);

    FromPath selectDistinct(Expression... expressions);

    FromPath selectDistinct(String... expressions);

    FromPath selectRaw(Expression expression);

    FromPath selectRaw(String expression);

}
