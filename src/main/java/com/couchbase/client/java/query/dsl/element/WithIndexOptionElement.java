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
import com.couchbase.client.java.document.json.JsonObject;

/**
 * Element of the Index DSL that allows to describe additional options for index creation.
 *
 * Options are set as a JSON object. Supported options as of Couchbase 4.0 DP are:
 *  - "nodes": "node_name": specify on which node to create a GSI index
 *  - "defer_build":true: defer creation of the index (useful to create multiple indexes then build them in one scan swipe).
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Private
public class WithIndexOptionElement implements Element {

    private final JsonObject options;

    public WithIndexOptionElement(JsonObject options) {
        this.options = options;
    }

    @Override
    public String export() {
        return "WITH " + options.toString();
    }
}
