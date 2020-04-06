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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * The property metadata implementation based on java reflection.
 *
 * @since 2.2.0
 */
public class ReflectionBasedPropertyMetadata implements PropertyMetadata {

    private final Field fieldReference;
    private final boolean isId;
    private final boolean isField;
    private final String name;
    private final String realName;
    private final String encryptionProviderName;

    public ReflectionBasedPropertyMetadata(final Field fieldReference) {
        this.fieldReference = fieldReference;

        isId = hasAnnotation(fieldReference, Id.class);
        isField = hasAnnotation(fieldReference, com.couchbase.client.java.repository.annotation.Field.class);
        EncryptedField encryptedField = findAnnotation(fieldReference, EncryptedField.class);
        this.encryptionProviderName = encryptedField != null ? encryptedField.provider() : null;

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
            findAnnotation(fieldReference, com.couchbase.client.java.repository.annotation.Field.class);
        if (annotation == null || annotation.value().isEmpty()) {
            return fieldReference.getName();
        }
        return annotation.value();
    }

    private static boolean hasAnnotation(AnnotatedElement element, Class<? extends Annotation> annotationClass) {
        return findAnnotation(element, annotationClass) != null;
    }

    /**
     * Searches the element for an annotation of the given class and returns the first match.
     * This is a recursive search that also considers meta-annotations.
     *
     * @param element the element to search
     * @param annotationClass the type of annotation to look for
     * @return Matching annotation, or null if not found.
     */
    private static <T extends Annotation> T findAnnotation(AnnotatedElement element, Class<T> annotationClass) {
        for (Annotation a : element.getAnnotations()) {
            T meta = findAnnotationRecursive(a, annotationClass, new HashSet<Class>());
            if (meta != null) {
                return meta;
            }
        }
        return null;
    }

    private static <T extends Annotation> T findAnnotationRecursive(Annotation annotation, Class<T> annotationClass, Set<Class> seen) {
        final Class c = annotation.annotationType();

        // Annotations can be annotated with themselves (@Documented is common example)
        // in which case we need to bail out to avoid stack overflow.
        if (!seen.add(c)) {
            return null;
        }

        if (c.equals(annotationClass)) {
            return annotationClass.cast(annotation);
        }

        for (Annotation meta : c.getAnnotations()) {
            T found = findAnnotationRecursive(meta, annotationClass, seen);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
