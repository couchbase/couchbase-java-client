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

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.repository.annotation.Id;
import com.couchbase.client.java.repository.mapping.RepositoryMappingException;
import com.couchbase.client.java.util.ClusterDependentTest;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class RepositoryTest extends ClusterDependentTest {

    @Test
    public void shouldUpsertAndGetEntity() {
        User user = new User("Michael", true, 1234, 55.6766);
        repository().upsert(user);

        JsonDocument storedRaw = bucket().get(user.id());
        assertEquals(user.name(), storedRaw.content().getString("name"));
        assertEquals(user.published(), storedRaw.content().getBoolean("published"));
        assertEquals(user.someNumber(), storedRaw.content().getInt("num"), 0);
        assertEquals(user.otherNumber(), storedRaw.content().getDouble("otherNumber"), 0);

        User found = repository().get(user.id(), User.class);
        assertEquals(user, found);
    }

    @Test(expected = RepositoryMappingException.class)
    public void shouldFailWithoutIdProperty() {
        repository().upsert(new EntityWithoutId());
    }

    @Test(expected = RepositoryMappingException.class)
    public void shouldFailWithNullIdProperty() {
        repository().upsert(new EntityWithId());
    }

    @Test(expected = RepositoryMappingException.class)
    public void shouldFailWithNonStringIdProperty() {
        repository().upsert(new EntityWithNoNStringId());
    }

    static class EntityWithoutId {
    }

    static class EntityWithId {
        public @Id String id = null;
    }

    static class EntityWithNoNStringId {
        public @Id Date id = new Date();
    }

}
