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

import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.WhereElement;

import static com.couchbase.client.java.query.dsl.Expression.x;


public class DefaultMergeInsertWherePath extends DefaultMutateLimitPath implements MergeInsertWherePath {

  public DefaultMergeInsertWherePath(AbstractPath parent) {
    super(parent);
  }

  @Override
  public MutateLimitPath where(Expression expression) {
    element(new WhereElement(expression));
    return new DefaultMutateLimitPath(this);
  }

  @Override
  public MutateLimitPath where(String expression) {
    return where(x(expression));
  }
}
