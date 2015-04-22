package com.couchbase.client.java.query.dsl.path;

import static com.couchbase.client.java.query.dsl.Expression.x;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.GroupByElement;

/**
 * .
 *
 * @author Michael Nitschinger
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class DefaultGroupByPath extends DefaultSelectResultPath implements GroupByPath {

    public DefaultGroupByPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public LettingPath groupBy(Expression... expressions) {
        element(new GroupByElement(expressions));
        return new DefaultLettingPath(this);
    }

    @Override
    public LettingPath groupBy(String... identifiers) {
        Expression[] expressions = new Expression[identifiers.length];
        for (int i = 0; i < identifiers.length; i++) {
            expressions[i] = x(identifiers[i]);
        }
        return groupBy(expressions);
    }
}
