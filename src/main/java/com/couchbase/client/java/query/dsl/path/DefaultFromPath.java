package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.FromElement;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DefaultFromPath extends DefaultLetPath implements FromPath {

    public DefaultFromPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public AsPath from(String from) {
        element(new FromElement(from));
        return new DefaultAsPath(this);
    }

    @Override
    public AsPath from(Expression from) {
        element(new FromElement(from.toString()));
        return new DefaultAsPath(this);
    }
}
