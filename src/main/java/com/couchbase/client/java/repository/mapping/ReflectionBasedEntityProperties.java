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
package com.couchbase.client.java.repository.mapping;

import com.couchbase.client.java.repository.annotation.Id;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Properties are gathered through reflection on the entity.
 *
 * @author Michael Nitschinger
 * @since 2.2.0
 */
public class ReflectionBasedEntityProperties implements EntityProperties {

    private Field idField;
    private final List<Field> fields;

    public ReflectionBasedEntityProperties(Class<?> source) {
        fields = new ArrayList<Field>();
        for (Field field : source.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                idField = field;
                field.setAccessible(true);
            } else if (field.isAnnotationPresent(com.couchbase.client.java.repository.annotation.Field.class)) {
                fields.add(field);
                field.setAccessible(true);
            }
        }
    }

    @Override
    public List<Field> fieldProperties() {
        return fields;
    }

    @Override
    public boolean hasIdProperty() {
        return idProperty() != null;
    }

    @Override
    public Field idProperty() {
        return idField;
    }


    @Override
    public String actualFieldPropertyName(Field field) {
        com.couchbase.client.java.repository.annotation.Field annotation =
            field.getDeclaredAnnotation(com.couchbase.client.java.repository.annotation.Field.class);

        if (annotation == null) {
            return field.getName();
        } else {
            String alias = annotation.value();
            return alias != null && !alias.isEmpty() ? alias : field.getName();
        }
    }

    @Override
    public <T> T get(Field field, Object source, Class<T> target) {
        try {
            return (T) field.get(source);
        } catch (IllegalAccessException e) {
            throw new RepositoryMappingException("Could not get field from entity.", e);
        }
    }

    @Override
    public void set(Field field, Object source, Object value) {
        try {
            field.set(source, value);
        } catch (IllegalAccessException e) {
            throw new RepositoryMappingException("Could not set field on entity.", e);
        }
    }
}
