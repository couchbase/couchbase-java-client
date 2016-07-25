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
package com.couchbase.client.java.cluster.api;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;

/**
 * Marker interface for {@link RestBuilder} and {@link AsyncRestBuilder}.
 * These builder classes can be used to incrementally construct REST API requests and execute
 * them. The execution is synchronous or asynchronous depending on the concrete builder type.
 *
 * @author Simon Baslé
 * @since 2.3.2
 */
@InterfaceAudience.Private
@InterfaceStability.Experimental
public interface RestBuilderMarker { }
