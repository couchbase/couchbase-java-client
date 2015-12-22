package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.element.LimitElement;


public class DefaultLimitPath extends DefaultOffsetPath implements LimitPath {

    public DefaultLimitPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public OffsetPath limit(int limit) {
        element(new LimitElement(limit));
        return new DefaultOffsetPath(this);
    }

}
