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
import com.couchbase.client.java.query.dsl.element.KeysElement;

import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;

public class DefaultDeleteUsePath extends DefaultMutateWherePath implements DeleteUsePath {

  public DefaultDeleteUsePath(AbstractPath parent) {
    super(parent);
  }

  @Override
  public MutateWherePath useKeys(Expression expression) {
    element(new KeysElement(KeysElement.ClauseType.USE_KEYSPACE, expression));
    return new DefaultMutateWherePath(this);
  }

  @Override
  public MutateWherePath useKeys(String key) {
    return useKeys(x(key));
  }

  @Override
  public MutateWherePath useKeysValues(String... keys) {
    if (keys.length == 1) {
      return useKeys(s(keys[0]));
    }
    return useKeys(JsonArray.from((Object[]) keys));
  }

  @Override
  public MutateWherePath useKeys(JsonArray keys) {
    return useKeys(x(keys));
  }
}
