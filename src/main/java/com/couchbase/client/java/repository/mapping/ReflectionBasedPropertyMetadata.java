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

import com.couchbase.client.java.repository.annotation.EncryptedField;
import com.couchbase.client.java.repository.annotation.Id;

import java.lang.reflect.Field;

/**
 * The property metadata implementation based on java reflection.
 *
 * @author Michael Nitschinger
 * @since 2.2.0
 */
public class ReflectionBasedPropertyMetadata implements PropertyMetadata {

    private final Field fieldReference;
    private final boolean isId;
    private final boolean isField;
    private final String name;
    private final String realName;
    private String encryptionProviderName;

    public ReflectionBasedPropertyMetadata(final Field fieldReference) {
        this.fieldReference = fieldReference;

        isId = fieldReference.isAnnotationPresent(Id.class);
        isField = fieldReference.isAnnotationPresent(com.couchbase.client.java.repository.annotation.Field.class);
        if (fieldReference.isAnnotationPresent(EncryptedField.class)) {
            EncryptedField encryptedField = fieldReference.getAnnotation(EncryptedField.class);
            this.encryptionProviderName = encryptedField.provider();
        }
        realName = fieldReference.getName();
        name = extractName(fieldReference);

        fieldReference.setAccessible(true);
    }

    @Override
    public boolean isId() {
        return isId;
    }

    @Override
    public boolean isField() {
        return isField;
    }

    @Override
    public String encryptionProviderName() {
        return this.encryptionProviderName;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String realName() {
        return realName;
    }

    @Override
    public Class<?> type() {
        return fieldReference.getType();
    }

    @Override
    public Object get(Object source) {
        try {
            return fieldReference.get(source);
        } catch (IllegalAccessException ex) {
            throw new RepositoryMappingException("Could not access field.", ex);
        }
    }

    @Override
    public void set(Object value, Object source) {
        try {
            fieldReference.set(source, value);
        } catch (IllegalAccessException ex) {
            throw new RepositoryMappingException("Could not access field.", ex);
        }
    }

    /**
     * Helper method to extract the potentially aliased name of the field.
     *
     * @param fieldReference the field reference.
     * @return the potentially aliased name of the field.
     */
    private static String extractName(final Field fieldReference) {
        com.couchbase.client.java.repository.annotation.Field annotation =
            fieldReference.getAnnotation(com.couchbase.client.java.repository.annotation.Field.class);
        if (annotation == null || annotation.value() == null || annotation.value().isEmpty()) {
            return fieldReference.getName();
        } else {
            return annotation.value();
        }
    }
}
