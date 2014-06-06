package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.element.AsElement;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DefaultAsPath extends DefaultKeysPath implements AsPath {

    public DefaultAsPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public KeysPath as(String alias) {
        element(new AsElement(alias));
        return new DefaultKeysPath(this);
    }

}
