/*
 * Copyright (c) 2016 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.Element;
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
            return onKeys(JsonArray.from((Object[]) constantKeys));
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
        return useKeys(JsonArray.from((Object[]) keys));
    }

    @Override
    public LetPath useKeys(JsonArray keys) {
        return useKeys(x(keys));
    }

    @Override
    public LetPath on(final Expression expression) {
        element(new Element() {
            @Override
            public String export() {
                return "ON " + expression.toString();
            }
        });
        return new DefaultLetPath(this);
    }
}
