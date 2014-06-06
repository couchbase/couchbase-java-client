package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.GroupByElement;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DefaultGroupByPath extends DefaultSelectResultPath implements GroupByPath {

    public DefaultGroupByPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public LettingPath groupBy(Expression... expressions) {
        element(new GroupByElement(expressions));
        return new DefaultLettingPath(this);
    }

}
