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
            if (propertyMetadata.isId()) {
                continue;
            }

            String name = propertyMetadata.name();
            Class<?> type = propertyMetadata.type();
            Object value = propertyMetadata.get(document);
            String encryptionProviderName = propertyMetadata.encryptionProviderName();

            if (value == null
                || value instanceof String
                || value instanceof Boolean
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Double) {
                if (encryptionProviderName != null) {
                    content.putAndEncrypt(name, value, encryptionProviderName);
                } else {
                    content.put(name, value);
                }
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
                    if (source.content().containsKey(JsonObject.ENCRYPTION_PREFIX + fieldName)) {
                        propertyMetadata.set(source.content().getAndDecrypt(fieldName, propertyMetadata.encryptionProviderName()), instance);
                    } else if(source.content().containsKey(fieldName)) {
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
