/**
 * Copyright (C) 2015 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */
package com.couchbase.client.java.query.dsl.path;


import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.KeysElement;

import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;

public class DefaultUpdateUsePath extends DefaultUpdateSetOrUnsetPath implements UpdateUsePath {

  public DefaultUpdateUsePath(AbstractPath parent) {
    super(parent);
  }

  @Override
  public UpdateSetOrUnsetPath useKeys(Expression expression) {
    element(new KeysElement(KeysElement.ClauseType.USE_KEYSPACE, expression));
    return new DefaultUpdateSetOrUnsetPath(this);
  }

  @Override
  public UpdateSetOrUnsetPath useKeys(String key) {
    return useKeys(x(key));
  }

  @Override
  public UpdateSetOrUnsetPath useKeysValues(String... keys) {
    if (keys.length == 1) {
      return useKeys(s(keys[0]));
    }
    return useKeys(JsonArray.from((Object[]) keys));
  }

  @Override
  public UpdateSetOrUnsetPath useKeys(JsonArray keys) {
    return useKeys(x(keys));
  }
}
