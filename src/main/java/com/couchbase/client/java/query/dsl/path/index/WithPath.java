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
import com.couchbase.client.java.query.dsl.path.Path;

/**
 * With path of the Index creation DSL (setting options).
 *
 * @author Simon Baslé
 * @since 2.2
 * @see WithIndexOptionElement
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public interface WithPath extends Path, Statement {

    /**
     * Specify on which node to create a GSI index.
     *
     * @param nodeName the name of the node on which to create an index.
     */
    Statement withNode(String nodeName);

    /**
     * Specify that the index creation should be deferred to later, allowing to create multiple index and then build
     * them all at once in one scan/swipe.
     */
    Statement withDefer();

    /**
     * Sets both index creation supported options : specify that the index creation should be deferred and give the
     * name of the node on which to create a GSI index.
     *
     * @param nodeName the name of the node on which to create an index.
     */
    Statement withDeferAndNode(String nodeName);

}
