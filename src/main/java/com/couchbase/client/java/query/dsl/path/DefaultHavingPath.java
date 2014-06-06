package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.HavingElement;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DefaultHavingPath extends DefaultSelectResultPath implements HavingPath {

    public DefaultHavingPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public SelectResultPath having(Expression condition) {
        element(new HavingElement(condition));
        return new DefaultSelectResultPath(this);
    }

}
