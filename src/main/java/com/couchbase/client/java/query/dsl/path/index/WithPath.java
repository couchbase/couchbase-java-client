/**
 * Copyright (C) 2015 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */
package com.couchbase.client.java.query.dsl.path.index;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.dsl.element.WithIndexOptionElement;
import com.couchbase.client.java.query.dsl.path.Path;

/**
 * With path of the Index creation DSL.
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
