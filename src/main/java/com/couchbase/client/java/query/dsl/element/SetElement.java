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
package com.couchbase.client.java.query.dsl.element;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.query.dsl.Expression;

@InterfaceStability.Experimental
@InterfaceAudience.Private
public class SetElement implements Element {

  private final Expression path;
  private final Expression setFor;
  private final SetPosition insert;
  private final Expression value;

  public SetElement(SetPosition insert, Expression path, Expression value, Expression setFor) {
    this.path = path;
    this.setFor = setFor;
    this.insert = insert;
    this.value = value;
  }

  @Override
  public String export() {
    String uf = setFor == null ? "" : " " + setFor.toString();
    return insert.repr + path.toString() + " = " + value.toString() + uf;
  }

  public enum SetPosition {
    INITIAL("SET "),
    NOT_INITIAL(", ");

    private final String repr;

    SetPosition(String repr) {
      this.repr = repr;
    }
  }

}
