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
import com.couchbase.client.java.query.dsl.element.Element;

import static com.couchbase.client.java.query.dsl.Expression.x;

public class DefaultMergeSourcePath extends AbstractPath implements MergeSourcePath {

  public DefaultMergeSourcePath(AbstractPath parent) {
    super(parent);
  }

  @Override
  public MergeKeyClausePath using(String source) {
    return using(x(source));
  }

  @Override
  public MergeKeyClausePath using(final Expression source) {
    element(new Element() {
      @Override
      public String export() {
        return "USING " + source.toString();
      }
    });
    return new DefaultMergeKeyClausePath(this);
  }

}
