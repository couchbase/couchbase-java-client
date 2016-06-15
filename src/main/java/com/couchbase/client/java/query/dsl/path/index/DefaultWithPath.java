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

import java.util.Collection;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonArray;
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

    private Statement with(boolean defer, String... nodeNames) {
        JsonObject options = JsonObject.create();
        if (defer) {
            options.put("defer_build", true);
        }
        if (nodeNames != null && nodeNames.length > 0) {
            options.put("nodes", JsonArray.from(nodeNames));
        }
        element(new WithIndexOptionElement(options));
        return this;
    }

    @Override
    public Statement withNode(String nodeName) {
        if (nodeName == null) {
            return with(false, (String[]) null);
        }
        return with(false, nodeName);
    }

    @Override
    public Statement withNodes(String... nodeNames) {
        return with(false, nodeNames);
    }

    @Override
    public Statement withNodes(Collection<String> nodeNames) {
        String[] nodeNamesArray;
        if (nodeNames == null || nodeNames.isEmpty()) {
            nodeNamesArray = null;
        } else {
            nodeNamesArray = nodeNames.toArray(new String[nodeNames.size()]);
        }
        return with(false, nodeNamesArray);
    }

    @Override
    public Statement withDefer() {
        return with(true, (String[]) null);
    }

    @Override
    public Statement withDeferAndNode(String nodeName) {
        if (nodeName == null) {
            return with(true, (String[]) null);
        }
        return with(true, nodeName);
    }

    @Override
    public Statement withDeferAndNodes(String... nodeNames) {
        return with(true, nodeNames);
    }

    @Override
    public Statement withDeferAndNodes(Collection<String> nodeNames) {
        String[] nodeNamesArray;
        if (nodeNames == null || nodeNames.isEmpty()) {
            nodeNamesArray = null;
        } else {
            nodeNamesArray = nodeNames.toArray(new String[nodeNames.size()]);
        }
        return with(true, nodeNamesArray);
    }

}
