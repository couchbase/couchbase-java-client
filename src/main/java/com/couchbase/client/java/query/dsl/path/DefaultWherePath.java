package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.WhereElement;

import static com.couchbase.client.java.query.dsl.Expression.x;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DefaultWherePath extends DefaultGroupByPath implements WherePath {

    public DefaultWherePath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public GroupByPath where(Expression expression) {
        element(new WhereElement(expression));
        return new DefaultGroupByPath(this);
    }

    @Override
    public GroupByPath where(String expression) {
        return where(x(expression));
    }

}
