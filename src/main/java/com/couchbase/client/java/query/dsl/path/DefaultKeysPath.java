package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.KeysElement;

import static com.couchbase.client.java.query.dsl.Expression.x;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DefaultKeysPath extends DefaultLetPath implements KeysPath {

    public DefaultKeysPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public LetPath keys(Expression expression) {
        element(new KeysElement(expression));
        return new DefaultLetPath(this);
    }

    @Override
    public LetPath keys(String key) {
        return keys(JsonArray.from(key));
    }

    @Override
    public LetPath keys(JsonArray keys) {
        return keys(x(keys));
    }
}
