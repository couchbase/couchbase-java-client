package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.element.AsElement;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DefaultUnnestPath extends DefaultLetPath implements UnnestPath {

    public DefaultUnnestPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public LetPath as(String alias) {
        element(new AsElement(alias));
        return new DefaultLetPath(this);
    }

}
