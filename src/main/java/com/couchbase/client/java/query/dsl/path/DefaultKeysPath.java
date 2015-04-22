package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.KeysElement;

import static com.couchbase.client.java.query.dsl.Expression.s;
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
    public LetPath onKeys(Expression expression) {
        element(new KeysElement(KeysElement.ClauseType.JOIN_ON, expression));
        return new DefaultLetPath(this);
    }

    @Override
    public LetPath onKeys(String key) {
        return onKeys(x(key));
    }

    @Override
    public LetPath onKeys(JsonArray keys) {
        return onKeys(x(keys));
    }

    @Override
    public LetPath onKeysValues(String... constantKeys) {
        if (constantKeys.length == 1) {
            return onKeys(s(constantKeys[0]));
        } else {
            return onKeys(JsonArray.from(constantKeys));
        }
    }

    @Override
    public LetPath useKeys(Expression expression) {
        element(new KeysElement(KeysElement.ClauseType.USE_KEYSPACE, expression));
        return new DefaultLetPath(this);
    }

    @Override
    public LetPath useKeys(String key) {
        return useKeys(x(key));
    }

    @Override
    public LetPath useKeysValues(String... keys) {
        if (keys.length == 1) {
            return useKeys(s(keys[0]));
        }
        return useKeys(JsonArray.from(keys));
    }

    @Override
    public LetPath useKeys(JsonArray keys) {
        return useKeys(x(keys));
    }
}
