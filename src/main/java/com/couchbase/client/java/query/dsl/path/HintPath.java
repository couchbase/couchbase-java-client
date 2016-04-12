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
package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.query.dsl.path.index.IndexReference;

/**
 * Hint clause (especially for specifying indexes to use) after a from clause.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public interface HintPath extends KeysPath {

    /**
     * Hint at what index(es) to use for this query. Use {@link IndexReference}'s factory methods.
     */
    KeysPath useIndex(IndexReference... indexes);

    /**
     * Hint at what index(es) to use for this query by giving names (will look for default index type).
     * The index names will be escaped.
     */
    KeysPath useIndex(String... indexes);
}
