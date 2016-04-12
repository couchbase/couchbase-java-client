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
package com.couchbase.client.java.repository.mapping;

import java.util.List;

/**
 * Represents the metadata for a document entity.
 *
 * @author Michael Nitschinger
 * @since 2.2.0
 */
public interface EntityMetadata {

    /**
     * Returns the metadata for all properties in this entity.
     *
     * @return the property information.
     */
    List<PropertyMetadata> properties();

    /**
     * True if it contains an id property, false otherwise.
     *
     * @return true if there is one, false otherwise.
     */
    boolean hasIdProperty();

    /**
     * Returns the metadata for the id property if set, or null otherwise.
     *
     * @return the metadata or null.
     */
    PropertyMetadata idProperty();

}
