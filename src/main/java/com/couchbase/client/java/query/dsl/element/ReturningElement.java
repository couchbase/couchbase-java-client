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
public class ReturningElement implements Element {

  private final ReturningType type;
  private final Expression exp;

  public ReturningElement(ReturningType type, Expression exp) {
    this.type = type;
    this.exp = exp;
  }

  @Override
  public String export() {
    return "RETURNING " + type.repr + exp.toString();
  }

  public enum ReturningType {
    REGULAR(""),
    RAW("RAW "),
    ELEMENT("ELEMENT ");

    private final String repr;

    ReturningType(String repr) {
      this.repr = repr;
    }
  }

}
