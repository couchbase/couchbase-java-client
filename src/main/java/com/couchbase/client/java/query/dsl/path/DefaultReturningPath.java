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

import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.ReturningElement;

import static com.couchbase.client.java.query.dsl.Expression.x;

public class DefaultReturningPath extends AbstractPath implements ReturningPath {

  public DefaultReturningPath(AbstractPath parent) {
    super(parent);
  }

  @Override
  public Statement returning(String expression) {
    element(new ReturningElement(ReturningElement.ReturningType.REGULAR, x(expression)));
    return this;
  }

  @Override
  public Statement returning(Expression expression) {
    element(new ReturningElement(ReturningElement.ReturningType.REGULAR, expression));
    return this;
  }

  @Override
  public Statement returningRaw(String expression) {
    element(new ReturningElement(ReturningElement.ReturningType.RAW, x(expression)));
    return this;
  }

  @Override
  public Statement returningRaw(Expression expression) {
    element(new ReturningElement(ReturningElement.ReturningType.RAW, expression));
    return this;
  }

  @Override
  public Statement returningElement(String expression) {
    element(new ReturningElement(ReturningElement.ReturningType.ELEMENT, x(expression)));
    return this;
  }

  @Override
  public Statement returningElement(Expression expression) {
    element(new ReturningElement(ReturningElement.ReturningType.ELEMENT, expression));
    return this;
  }

}
