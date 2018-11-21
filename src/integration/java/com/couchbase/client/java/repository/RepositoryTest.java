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
package com.couchbase.client.java.repository;

import com.couchbase.client.java.document.EntityDocument;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.repository.annotation.Field;
import com.couchbase.client.java.repository.annotation.Id;
import com.couchbase.client.java.repository.mapping.RepositoryMappingException;
import com.couchbase.client.java.util.ClusterDependentTest;
import com.couchbase.client.java.util.CouchbaseTestContext;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

public class RepositoryTest extends ClusterDependentTest {

    @BeforeClass
    public static void check() {
        assumeFalse(CouchbaseTestContext.isCi());
    }

    @Test
    public void shouldUpsertAndGetEntity() {
        User entity = new User("Michael", true, 1234, 55.6766);
        EntityDocument<User> document = EntityDocument.create(entity.id(), entity);

        assertFalse(repository().exists(document));
        assertEquals(0, document.cas());
        EntityDocument<User> stored = repository().upsert(document);
        assertNotEquals(0, stored.cas());
        assertEquals(document.content(), stored.content());

        JsonDocument storedRaw = bucket().get(entity.id());
        assertEquals(entity.name(), storedRaw.content().getString("name"));
        assertEquals(entity.published(), storedRaw.content().getBoolean("published"));
        assertEquals(entity.someNumber(), storedRaw.content().getInt("num"), 0);
        assertEquals(entity.otherNumber(), storedRaw.content().getDouble("otherNumber"), 0);

        EntityDocument<User> found = repository().get(entity.id(), User.class);
        assertEquals(found.cas(), stored.cas());
        assertEquals(entity, found.content());
        assertNotEquals(0, found.cas());

        assertTrue(repository().exists(document));
    }

    @Test
    public void shouldInsertEntity() {
        User entity = new User("Tom", false, -34, -55.6766);
        EntityDocument<User> document = EntityDocument.create(entity.id(), entity);

        assertFalse(repository().exists(document));
        EntityDocument<User> stored = repository().insert(document);
        assertEquals(document.content(), stored.content());

        JsonDocument storedRaw = bucket().get(entity.id());
        assertEquals(entity.name(), storedRaw.content().getString("name"));
        assertEquals(entity.published(), storedRaw.content().getBoolean("published"));
        assertEquals(entity.someNumber(), storedRaw.content().getInt("num"), 0);
        assertEquals(entity.otherNumber(), storedRaw.content().getDouble("otherNumber"), 0);

        EntityDocument<User> found = repository().get(entity.id(), User.class);
        assertEquals(entity, found.content());
        assertNotEquals(0, found.cas());

        assertTrue(repository().exists(document));
    }

    @Test
    public void shouldReplaceEntity() {
        User entity = new User("John", false, -34, -55.6766);
        EntityDocument<User> document = EntityDocument.create(entity.id(), entity);

        assertFalse(repository().exists(document));
        EntityDocument<User> stored = repository().upsert(document);
        assertEquals(document.content(), stored.content());

        entity = new User("John", true, 0, 0);
        document = EntityDocument.create(entity.id(), entity);

        EntityDocument<User> replaced = repository().replace(document);
        assertEquals(entity, replaced.content());

        JsonDocument storedRaw = bucket().get(entity.id());
        assertEquals(entity.name(), storedRaw.content().getString("name"));
        assertEquals(entity.published(), storedRaw.content().getBoolean("published"));
        assertEquals(entity.someNumber(), storedRaw.content().getInt("num"), 0);
        assertEquals(entity.otherNumber(), storedRaw.content().getDouble("otherNumber"), 0);

        EntityDocument<User> found = repository().get(entity.id(), User.class);
        assertEquals(entity, found.content());
        assertNotEquals(0, found.cas());

        assertTrue(repository().exists(document));
    }

    @Test
    public void shouldRemoveEntity() {
        User entity = new User("Jane", false, -34, -55.6766);
        EntityDocument<User> document = EntityDocument.create(entity.id(), entity);

        assertFalse(repository().exists(document));
        EntityDocument<User> stored = repository().upsert(document);
        assertEquals(entity, stored.content());
        assertTrue(repository().exists(stored));

        EntityDocument<User> removed = repository().remove(stored);
        assertFalse(repository().exists(removed));
        assertEquals(document.id(), removed.id());
    }

    @Test(expected = RepositoryMappingException.class)
    public void shouldFailWithoutIdProperty() {
        repository().upsert(EntityDocument.create(new EntityWithoutId()));
    }

    @Test(expected = RepositoryMappingException.class)
    public void shouldFailWithNullIdProperty() {
        repository().upsert(EntityDocument.create(new EntityWithId()));
    }

    @Test(expected = RepositoryMappingException.class)
    public void shouldFailWithNonStringIdProperty() {
        repository().upsert(EntityDocument.create(new EntityWithNoNStringId()));
    }

    @Test
    public void shouldUpsertExtendedEntity() {
        Child entity = new Child("myid", "myname");
        EntityDocument<Child> document = EntityDocument.create(entity);

        assertFalse(repository().exists(document));
        assertEquals(0, document.cas());
        EntityDocument<Child> stored = repository().upsert(document);
        assertNotEquals(0, stored.cas());
        assertEquals(document.content(), stored.content());

        JsonDocument storedRaw = bucket().get(entity.getId());
        assertEquals(entity.getName(), storedRaw.content().getString("name"));

        EntityDocument<Child> found = repository().get(entity.getId(), Child.class);
        assertEquals(found.cas(), stored.cas());
        assertNotEquals(0, found.cas());

        assertTrue(repository().exists(document));
    }

    static class EntityWithoutId {
    }

    static class EntityWithId {
        public @Id
        String id = null;
    }

    static class EntityWithNoNStringId {
        public @Id
        Date id = new Date();
    }


    public static abstract class Parent {
         @Id
         private String id;

         Parent(String id) {
            this.id = id;
         }

        public String getId() {
            return id;
        }
    }

    public static class Child extends Parent {
        @Field
        private String name;

        public Child() {
            super(null);
        }

        public Child(String id, String name) {
            super(id);
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}
