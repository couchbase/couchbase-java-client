/*
 * Copyright (c) 2017 Couchbase, Inc.
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
package com.couchbase.client.java;

import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.JsonLongDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.TemporaryFailureException;
import com.couchbase.client.java.error.TemporaryLockFailureException;
import com.couchbase.client.java.util.CouchbaseTestContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test explicitly tests the old/new locking response code handling between
 * Couchbase Server 5.0 and earlier.
 *
 * @author Michael Nitschinger
 * @since 2.5.0
 */
public class LockingTest {

    private static CouchbaseTestContext ctx;

    @BeforeClass
    public static void connect() throws Exception {
        ctx = CouchbaseTestContext.builder()
            .bucketQuota(256)
            .bucketType(BucketType.COUCHBASE)
            .flushOnInit(true)
            .enableFlush(true)
            .build();
    }

    @AfterClass
    public static void disconnect() throws InterruptedException {
        ctx.disconnect();
    }

    @Test(expected = CASMismatchException.class)
    public void shouldHandleLockOnUpsert() {
        String id = createAndLockDoc();
        ctx.bucket().upsert(JsonDocument.create(id, JsonObject.empty()));
    }

    @Test(expected = CASMismatchException.class)
    public void shouldHandleLockOnReplace() {
        String id = createAndLockDoc();
        ctx.bucket().replace(JsonDocument.create(id, JsonObject.empty()));
    }

    @Test(expected = CASMismatchException.class)
    public void shouldHandleLockOnRemove() {
        String id = createAndLockDoc();
        ctx.bucket().replace(JsonDocument.create(id, JsonObject.empty()));
    }

    @Test(expected = TemporaryFailureException.class)
    public void shouldHandleLockOnGetAndTouch() {
        String id = createAndLockDoc();
        ctx.bucket().getAndTouch(id, 10);
    }

    @Test(expected = TemporaryLockFailureException.class)
    public void shouldHandleLockOnGetAndLock() {
        String id = createAndLockDoc();
        ctx.bucket().getAndLock(id, 10);
    }

    @Test(expected = TemporaryFailureException.class)
    public void shouldHandleLockOnTouch() {
        String id = createAndLockDoc();
        ctx.bucket().touch(id, 10);
    }

    @Test(expected = TemporaryFailureException.class)
    public void shouldHandleLockOnAppend() {
        String id = createAndLockDoc();
        ctx.bucket().append(JsonDocument.create(id, JsonObject.empty()));
    }

    @Test(expected = TemporaryFailureException.class)
    public void shouldHandleLockOnPrepend() {
        String id = createAndLockDoc();
        ctx.bucket().prepend(JsonDocument.create(id, JsonObject.empty()));
    }

    @Test(expected = TemporaryLockFailureException.class)
    public void shouldHandleLockOnUnlock() {
        String id = createAndLockDoc();
        ctx.bucket().unlock(id, -1);
    }

    @Test(expected = TemporaryFailureException.class)
    public void shouldHandleLockOnCounter() {
        String id = UUID.randomUUID().toString();
        assertEquals(id, ctx.bucket().counter(id, 10, 0).id());
        assertTrue(ctx.bucket().getAndLock(id, 30, JsonLongDocument.class).cas() != 0);
        ctx.bucket().counter(id, 10);
    }

    @Test(expected = TemporaryFailureException.class)
    public void shouldHandleLockOnMutateIn() {
        String id = createAndLockDoc();
        ctx.bucket().mutateIn(id).upsert("foo", "bar").execute();
    }

    /**
     * Helper method to create a new doc and lock it.
     *
     * @return the doc id.
     */
    private String createAndLockDoc() {
        String id = UUID.randomUUID().toString();
        assertEquals(id, ctx.bucket().upsert(JsonDocument.create(id, JsonObject.empty())).id());
        assertTrue(ctx.bucket().getAndLock(id, 30).cas() != 0);
        return id;
    }
}
