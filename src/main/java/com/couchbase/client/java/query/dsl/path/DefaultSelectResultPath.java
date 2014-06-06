package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.element.UnionElement;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DefaultSelectResultPath extends DefaultOrderByPath implements SelectResultPath {

    public DefaultSelectResultPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public SelectPath union() {
        element(new UnionElement(false));
        return new DefaultSelectPath(this);
    }

    @Override
    public SelectPath unionAll() {
        element(new UnionElement(true));
        return new DefaultSelectPath(this);
    }

}
