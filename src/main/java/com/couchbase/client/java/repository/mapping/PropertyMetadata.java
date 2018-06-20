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

/**
 * Represents the metadata for a document property inside an {@link EntityMetadata}.
 *
 * @author Michael Nitschinger
 * @since 2.2.0
 */
public interface PropertyMetadata {

    /**
     * If this property represents the Document ID.
     *
     * @return true if it does, false otherwise.
     */
    boolean isId();

    /**
     * If this property represents a field in the Document.
     *
     * @return true if it does, false otherwise.
     */
    boolean isField();


    /**
     * The name of the field inside the document.
     *
     * If an alias is used, it is reflected in here. If the raw
     * field name in the entity is needed, use {@link #realName()}.
     *
     * @return the name of the field.
     */
    String name();

    /**
     * The name of the actual property inside the java entity.
     *
     * @return the real field name.
     */
    String realName();

    /**
     * Returns the content of the field property.
     *
     * @param source the source object.
     * @return the content of the field.
     */
    Object get(Object source);

    /**
     * Sets the content of the field property.
     *
     * @param value the value to set.
     * @param source the source object.
     */
    void set(Object value, Object source);

    /**
     * The type of the field property.
     *
     * @return the type.
     */
    Class<?> type();

    /**
     * Get the encryption crypto algorithm provider name set
     *
     * @return the crypto provider name
     */
    String encryptionProviderName();
}
