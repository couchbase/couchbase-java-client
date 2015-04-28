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
package com.couchbase.client.java.repository;

import com.couchbase.client.java.document.EntityDocument;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.repository.annotation.Id;
import com.couchbase.client.java.repository.mapping.RepositoryMappingException;
import com.couchbase.client.java.util.ClusterDependentTest;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class RepositoryTest extends ClusterDependentTest {

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

}
