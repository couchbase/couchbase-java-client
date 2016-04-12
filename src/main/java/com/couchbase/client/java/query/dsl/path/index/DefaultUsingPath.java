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
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.dsl.element.UsingElement;
import com.couchbase.client.java.query.dsl.path.AbstractPath;

/**
 * See {@link UsingPath}.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Private
public class DefaultUsingPath extends AbstractPath implements UsingPath {

    protected DefaultUsingPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public Statement using(IndexType indexType) {
        element(new UsingElement(indexType));
        return this;
    }
}
