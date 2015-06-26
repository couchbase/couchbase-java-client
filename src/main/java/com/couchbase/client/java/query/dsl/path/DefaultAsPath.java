package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.element.AsElement;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DefaultAsPath extends DefaultHintPath implements AsPath {

    public DefaultAsPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public HintPath as(String alias) {
        element(new AsElement(alias));
        return new DefaultHintPath(this);
    }

}
