package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.element.OrderByElement;
import com.couchbase.client.java.query.dsl.Sort;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DefaultOrderByPath extends DefaultLimitPath implements OrderByPath {

    public DefaultOrderByPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public LimitPath orderBy(Sort... orderings) {
        element(new OrderByElement(orderings));
        return new DefaultLimitPath(this);
    }

}
