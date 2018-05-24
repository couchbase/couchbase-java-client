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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reflection based implementation for entity metadata.
 *
 * @author Michael Nitschinger
 * @since 2.2.0
 */
public class ReflectionBasedEntityMetadata implements EntityMetadata {

    private final List<PropertyMetadata> properties;
    private final PropertyMetadata idProperty;

    public ReflectionBasedEntityMetadata(final Class<?> sourceEntity) {
        properties = new ArrayList<PropertyMetadata>();

        PropertyMetadata idProperty = null;
        for (Field field : getAllDeclaredFields(sourceEntity)) {
            PropertyMetadata property = new ReflectionBasedPropertyMetadata(field);
            properties.add(property);
            if (property.isId()) {
                idProperty = property;
            }
        }

        this.idProperty = idProperty;
    }

    /**
     * Helper method to grab all the declared fields from the given class but also
     * from its inherited parents!
     *
     * @param sourceEntity the source entity to start from.
     * @return an iterable of found fields.
     */
    private static List<Field> getAllDeclaredFields(final Class<?> sourceEntity) {
        List<Field> fields = new ArrayList<Field>();
        Class<?> clazz = sourceEntity;
        while (clazz != null) {
            Field[] f = clazz.getDeclaredFields();
            fields.addAll(Arrays.asList(f));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    @Override
    public List<PropertyMetadata> properties() {
        return properties;
    }

    @Override
    public boolean hasIdProperty() {
        return idProperty != null;
    }

    @Override
    public PropertyMetadata idProperty() {
        return idProperty;
    }

}
