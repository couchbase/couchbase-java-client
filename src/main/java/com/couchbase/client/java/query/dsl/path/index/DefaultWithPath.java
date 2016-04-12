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
package com.couchbase.client.java.query.dsl.path.index;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.dsl.element.WithIndexOptionElement;
import com.couchbase.client.java.query.dsl.path.AbstractPath;

/**
 * See {@link WithPath}.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Private
public class DefaultWithPath extends AbstractPath implements WithPath {

    public DefaultWithPath(AbstractPath parent) {
        super(parent);
    }

    private Statement with(boolean defer, String nodeName) {
        JsonObject options = JsonObject.create();
        if (defer) {
            options.put("defer_build", true);
        }
        if (nodeName != null) {
            options.put("nodes", nodeName);
        }
        element(new WithIndexOptionElement(options));
        return this;
    }

    @Override
    public Statement withNode(String nodeName) {
        return with(false, nodeName);
    }

    @Override
    public Statement withDefer() {
        return with(true, null);
    }

    @Override
    public Statement withDeferAndNode(String nodeName) {
        return with(true, nodeName);
    }
}
