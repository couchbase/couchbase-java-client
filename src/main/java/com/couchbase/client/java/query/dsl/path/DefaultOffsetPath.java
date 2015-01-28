package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.dsl.element.OffsetElement;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DefaultOffsetPath extends AbstractPath implements OffsetPath {

    public DefaultOffsetPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public Statement offset(int offset) {
        element(new OffsetElement(offset));
        return this;
    }

}
