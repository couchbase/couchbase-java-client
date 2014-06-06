package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Alias;
import com.couchbase.client.java.query.dsl.element.LettingElement;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DefaultLettingPath extends DefaultHavingPath implements LettingPath {

    public DefaultLettingPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public HavingPath letting(Alias... aliases) {
        element(new LettingElement(aliases));
        return new DefaultHavingPath(this);
    }
}
