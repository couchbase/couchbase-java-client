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
import com.couchbase.client.java.query.dsl.element.BuildIndexElement;
import com.couchbase.client.java.query.dsl.path.AbstractPath;

/**
 * see {@link BuildIndexPath}
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Private
public class DefaultBuildIndexPath extends AbstractPath implements BuildIndexPath {

    public DefaultBuildIndexPath() {
        super(null);
    }

    @Override
    public IndexNamesPath on(String namespace, String keyspace) {
        element(new BuildIndexElement(namespace, keyspace));
        return new DefaultIndexNamesPath(this);
    }

    @Override
    public IndexNamesPath on(String keyspace) {
        element(new BuildIndexElement(null, keyspace));
        return new DefaultIndexNamesPath(this);
    }
}
