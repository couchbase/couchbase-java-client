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
import com.couchbase.client.java.query.dsl.path.Path;

/**
 * Starting path of the Index creation DSL.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public interface CreateIndexPath extends Path {

    /**
     * Create a secondary index.
     * @param indexName the name of the secondary index to be created. It will automatically be escaped.
     */
    OnPath create(String indexName);

    /**
     * Create a primary index.
     */
    OnPrimaryPath createPrimary();

    /**
     * Create a primary index with a custom name.
     * @param customPrimaryName the custom name for the primary index.
     */
    OnPrimaryPath createPrimary(String customPrimaryName);

}
