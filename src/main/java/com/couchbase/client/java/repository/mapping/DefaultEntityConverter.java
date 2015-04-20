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

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultEntityConverter implements EntityConverter<JsonDocument> {

    private final Map<Class<?>, EntityMetadata> metadataCache;

    public DefaultEntityConverter() {
        this.metadataCache = new ConcurrentHashMap<Class<?>, EntityMetadata>();
    }

    @Override
    public JsonDocument fromEntity(Object document) {
        EntityMetadata entityMetadata = metadata(document.getClass());

        if (!entityMetadata.hasIdProperty()) {
            throw new RepositoryMappingException("No Id Field annotated with @Id present.");
        }

        String id = (String) entityMetadata.idProperty().get(document);
        if (id == null) {
            throw new RepositoryMappingException("Id Field cannot be null.");
        }

        JsonObject content = JsonObject.create();

        for (PropertyMetadata propertyMetadata : entityMetadata.properties()) {
            String name = propertyMetadata.name();
            Class<?> type = propertyMetadata.type();
            Object value = propertyMetadata.get(document);

            if (value == null
                || value instanceof String
                || value instanceof Boolean
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Double) {
                content.put(name, value);
            } else {
                throw new RepositoryMappingException("Unsupported field type: " + type);
            }
        }
        return JsonDocument.create(id, content);
    }

    @Override
    public <T> T toEntity(JsonDocument source, Class<T> clazz) {
        try {
            EntityMetadata entityMetadata = metadata(clazz);

            T instance = clazz.newInstance(); // for now only support no-args constructor

            for (PropertyMetadata propertyMetadata : entityMetadata.properties()) {
                String fieldName = propertyMetadata.name();
                if (source.content().containsKey(fieldName)) {
                    propertyMetadata.set(source.content().get(fieldName), instance);
                }
            }

            if (!entityMetadata.hasIdProperty()) {
                throw new RepositoryMappingException("No Id Field annotated with @Id present.");
            }

            entityMetadata.idProperty().set(source.id(), instance);
            return instance;
        } catch (Exception e) {
            throw new RepositoryMappingException("Could not instantiate entity.", e);
        }
    }

    /**
     * Helper method to return and cache the entity metadata.
     *
     * @param source the source class.
     * @return the metadata.
     */
    private EntityMetadata metadata(final Class<?> source) {
        EntityMetadata metadata = metadataCache.get(source);

        if (metadata == null) {
            EntityMetadata generated = new ReflectionBasedEntityMetadata(source);
            metadataCache.put(source, generated);
            return generated;
        } else {
            return metadata;
        }
    }
}
