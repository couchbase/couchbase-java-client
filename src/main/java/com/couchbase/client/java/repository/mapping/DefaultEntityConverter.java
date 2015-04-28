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

import com.couchbase.client.java.document.EntityDocument;
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
    public JsonDocument fromEntity(EntityDocument<Object> source) {
        Object document = source.content();
        EntityMetadata entityMetadata = metadata(document.getClass());

        String id = source.id();
        if (id == null) {
            verifyId(entityMetadata);
            id = (String) entityMetadata.idProperty().get(document);
            if (id == null || id.isEmpty()) {
                throw new RepositoryMappingException("The @Id field cannot be null or empty.");
            }
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
        return JsonDocument.create(id, source.expiry(), content, source.cas());
    }

    @Override
    public <T> EntityDocument<T> toEntity(JsonDocument source, Class<T> clazz) {
        try {
            EntityMetadata entityMetadata = metadata(clazz);

            T instance = clazz.newInstance(); // for now only support no-args constructor

            if (source.content() != null) {
                for (PropertyMetadata propertyMetadata : entityMetadata.properties()) {
                    String fieldName = propertyMetadata.name();
                    if (source.content().containsKey(fieldName)) {
                        propertyMetadata.set(source.content().get(fieldName), instance);
                    }
                }
            }

            if (entityMetadata.hasIdProperty()) {
                entityMetadata.idProperty().set(source.id(), instance);
            }

            return EntityDocument.create(source.id(), source.expiry(), instance, source.cas());
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


    /**
     * Helper method to check that the ID field is present and is of the desired types.
     *
     * @param entityMetadata the entity metadata.
     */
    private static void verifyId(final EntityMetadata entityMetadata) {
        if (!entityMetadata.hasIdProperty()) {
            throw new RepositoryMappingException("No field annotated with @Id present.");
        }

        if (entityMetadata.idProperty().type() != String.class) {
            throw new RepositoryMappingException("The @Id Field needs to be of type String.");
        }
    }
}
