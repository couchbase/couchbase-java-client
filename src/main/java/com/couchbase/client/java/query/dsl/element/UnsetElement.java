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
public class UnsetElement implements Element {

  private final Expression path;
  private final Expression unsetFor;
  private final UnsetPosition insert;

  public UnsetElement(UnsetPosition insert, Expression path, Expression unsetFor) {
    this.path = path;
    this.unsetFor = unsetFor;
    this.insert = insert;
  }

  @Override
  public String export() {
    String uf = unsetFor == null ? "" : " " + unsetFor.toString();
    return insert.repr + path.toString() + uf;
  }

  public enum UnsetPosition {
    INITIAL("UNSET "),
    NOT_INITIAL(", ");

    private final String repr;

    UnsetPosition(String repr) {
      this.repr = repr;
    }
  }

}
