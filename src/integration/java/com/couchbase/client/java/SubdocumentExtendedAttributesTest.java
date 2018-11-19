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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.util.Arrays;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.subdoc.multi.Lookup;
import com.couchbase.client.core.message.kv.subdoc.multi.Mutation;
import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.subdoc.DocumentFragment;
import com.couchbase.client.java.subdoc.SubdocOptionsBuilder;
import com.couchbase.client.java.util.CouchbaseTestContext;
import com.couchbase.client.java.util.features.CouchbaseFeature;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the extended attributes access from sub-document API in {@link Bucket}.
 *
 * @author Subhashni Balakrishnan
 */
public class SubdocumentExtendedAttributesTest {

    private static CouchbaseTestContext ctx;
    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(SubdocumentExtendedAttributesTest.class);

    private String key = "SubdocXAttrAPI";
    private JsonObject testJson;

    @BeforeClass
    public static void connect() throws Exception {
        ctx = CouchbaseTestContext.builder()
                .bucketQuota(100)
                .bucketReplicas(1)
                .bucketType(BucketType.COUCHBASE)
                .build();

        // Extended attributes is intended to be enabled by default,
        // but now it is turned on by setting memcached env variable
        // COUCHBASE_FORCE_ENABLE_XATTR=1 in initd script
        ctx.ignoreIfMissing(CouchbaseFeature.XATTR);
    }

    @Before
    public void initData() {
        testJson = JsonObject.create()
                .put("sub", JsonObject.create().put("value", "original"))
                .put("boolean", true)
                .put("string", "someString")
                .put("int", 123)
                .put("array", JsonArray.from("1", 2, true));

        ctx.bucket().upsert(JsonDocument.create(key, testJson));
    }

    @AfterClass
    public static void cleanup() {
        if (ctx != null) {
            ctx.disconnect();
        }
    }

    @Test
    public void shouldBeAbleToPersistXATTR() {
        DocumentFragment<Mutation> result = ctx.bucket()
                .mutateIn(key)
                .upsert("spring.class", "SomeClass", new SubdocOptionsBuilder().createPath(true).xattr(true))
                .execute(PersistTo.ONE);

        assertTrue(result.cas() != 0);
        assertEquals(ResponseStatus.SUCCESS, result.status("spring.class"));

        DocumentFragment<Lookup> lookupResult = ctx.bucket()
                .lookupIn(key)
                .get("spring.class", new SubdocOptionsBuilder().xattr(true))
                .execute();
        assertTrue(lookupResult.cas() != 0);
        assertEquals(ResponseStatus.SUCCESS, lookupResult.status("spring.class"));
    }

    @Test
    public void shouldNotBeAbleToGetXATTRWithoutAccessFlagSet() {
        DocumentFragment<Mutation> result = ctx.bucket()
                .mutateIn(key)
                .upsert("spring.class", "SomeClass", new SubdocOptionsBuilder().createPath(true).xattr(true))
                .execute(PersistTo.ONE);

        assertTrue(result.cas() != 0);
        assertEquals(ResponseStatus.SUCCESS, result.status("spring.class"));

        DocumentFragment<Lookup> lookupResult = ctx.bucket()
                .lookupIn(key)
                .get("spring.class", new SubdocOptionsBuilder().xattr(false))
                .execute();
        assertEquals(ResponseStatus.SUBDOC_PATH_NOT_FOUND, lookupResult.status("spring.class"));
    }

    @Test
    public void verifyExistXATTR() {
        DocumentFragment<Mutation> result = ctx.bucket()
                .mutateIn(key)
                .upsert("spring.class", "SomeClass", new SubdocOptionsBuilder().createPath(true).xattr(true))
                .execute(PersistTo.ONE);

        assertTrue(result.cas() != 0);
        assertEquals(ResponseStatus.SUCCESS, result.status("spring.class"));

        DocumentFragment<Lookup> lookupResult = ctx.bucket()
                .lookupIn(key)
                .exists("spring.class", new SubdocOptionsBuilder().xattr(true))
                .execute();
        assertEquals(ResponseStatus.SUCCESS, lookupResult.status("spring.class"));
    }


    @Test
    public void verifyArrayOpsXATTR() {
        DocumentFragment<Mutation> result = ctx.bucket()
                .mutateIn(key)
                .arrayAddUnique("spring.refs", "id1", new SubdocOptionsBuilder().createPath(true).xattr(true))
                .execute();

        assertTrue(result.cas() != 0);
        assertEquals(ResponseStatus.SUCCESS, result.status("spring.refs"));


        DocumentFragment<Lookup> lookupResult = ctx.bucket()
                .lookupIn(key)
                .exists("spring.refs", new SubdocOptionsBuilder().xattr(true))
                .execute();
        assertEquals(ResponseStatus.SUCCESS, lookupResult.status("spring.refs"));

        result = ctx.bucket()
                .mutateIn(key)
                .arrayAppend("spring.refs", "id0", new SubdocOptionsBuilder().createPath(false).xattr(true))
                .execute();
        assertTrue(result.cas() != 0);
        assertEquals(ResponseStatus.SUCCESS, result.status("spring.refs"));

        result = ctx.bucket()
                .mutateIn(key)
                .arrayPrepend("spring.refs", "id2", new SubdocOptionsBuilder().createPath(false).xattr(true))
                .execute();
        assertTrue(result.cas() != 0);
        assertEquals(ResponseStatus.SUCCESS, result.status("spring.refs"));

        result = ctx.bucket()
                .mutateIn(key)
                .arrayInsert("spring.refs[0]", "id3", new SubdocOptionsBuilder().createPath(false).xattr(true))
                .execute();
        assertTrue(result.cas() != 0);
        assertEquals(ResponseStatus.SUCCESS, result.status("spring.refs[0]"));

        result = ctx.bucket()
                .mutateIn(key)
                .remove("spring.refs[0]", new SubdocOptionsBuilder().createPath(false).xattr(true))
                .execute();
        assertTrue(result.cas() != 0);
        assertEquals(ResponseStatus.SUCCESS, result.status("spring.refs[0]"));

        result = ctx.bucket()
                .mutateIn(key)
                .remove("spring.refs", new SubdocOptionsBuilder().createPath(false).xattr(true))
                .execute();
        assertTrue(result.cas() != 0);
        assertEquals(ResponseStatus.SUCCESS, result.status("spring.refs"));
    }

    @Test
    public void verifyChainArrayOpsXATTR() {
        DocumentFragment<Mutation> result = ctx.bucket()
                .mutateIn(key)
                .arrayAddUnique("spring.refs", "id1", new SubdocOptionsBuilder().createPath(true).xattr(true))
                .arrayAppend("spring.refs", "id0", new SubdocOptionsBuilder().createPath(false).xattr(true))
                .arrayPrepend("spring.refs", "id2", new SubdocOptionsBuilder().createPath(false).xattr(true))
                .arrayInsert("spring.refs[0]", "id3", new SubdocOptionsBuilder().createPath(false).xattr(true))
                .remove("spring.refs[0]", new SubdocOptionsBuilder().createPath(false).xattr(true))
                .remove("spring.refs", new SubdocOptionsBuilder().createPath(false).xattr(true))
                .execute();
        assertTrue(result.cas() != 0);
        assertEquals(ResponseStatus.SUCCESS, result.status("spring.refs"));
        assertEquals(ResponseStatus.SUCCESS, result.status("spring.refs[0]"));
    }

    @Test
    public void verifyChainArrayCollectionOpsXATTR() {
        assumeFalse(CouchbaseTestContext.isMockEnabled());

        DocumentFragment<Mutation> result = ctx.bucket()
                .mutateIn(key)
                .arrayAppendAll("spring.refs", Arrays.asList("id4", "id5", "id6", "id7"), new SubdocOptionsBuilder().createPath(true).xattr(true))
                .arrayPrependAll("spring.refs", Arrays.asList("id0", "id1", "id2", "id3"), new SubdocOptionsBuilder().createPath(false).xattr(true))
                .arrayInsertAll("spring.refs[0]",  Arrays.asList("id8", "id9", "id10", "id11"), new SubdocOptionsBuilder().createPath(false).xattr(true))
                .execute();
        assertTrue(result.cas() != 0);
        assertEquals(ResponseStatus.SUCCESS, result.status("spring.refs"));

        DocumentFragment<Lookup> lookupResult = ctx.bucket()
                .lookupIn(key)
                .get("spring.refs",new SubdocOptionsBuilder().createPath(false).xattr(true))
                .execute();
        assertEquals(12, ((JsonArray)lookupResult.content("spring.refs")).size());


        result = ctx.bucket()
                .mutateIn(key)
                .remove("spring.refs", new SubdocOptionsBuilder().createPath(false).xattr(true))
                .execute();
        assertTrue(result.cas() != 0);
        assertEquals(ResponseStatus.SUCCESS, result.status("spring.refs"));
    }

    @Test
    public void verifyDictOpsXATTR() {
        DocumentFragment<Mutation> result = ctx.bucket()
                .mutateIn(key)
                .insert("spring.dict.foo1", "bar1", new SubdocOptionsBuilder().createPath(true).xattr(true))
                .execute();

        assertTrue(result.cas() != 0);
        assertEquals(ResponseStatus.SUCCESS, result.status("spring.dict.foo1"));

        result = ctx.bucket()
                .mutateIn(key)
                .insert("spring.dict.foo2", "bar2", new SubdocOptionsBuilder().createPath(false).xattr(true))
                .execute();
        assertTrue(result.cas() != 0);
        assertEquals(ResponseStatus.SUCCESS, result.status("spring.dict.foo2"));

        result = ctx.bucket()
                .mutateIn(key)
                .upsert("spring.dict.foo1", 0, new SubdocOptionsBuilder().createPath(false).xattr(true))
                .execute();
        assertTrue(result.cas() != 0);
        assertEquals(ResponseStatus.SUCCESS, result.status("spring.dict.foo1"));

        result = ctx.bucket()
                .mutateIn(key)
                .counter("spring.dict.foo1", 1, new SubdocOptionsBuilder().createPath(false).xattr(true))
                .execute();
        assertTrue(result.cas() != 0);
        assertEquals(ResponseStatus.SUCCESS, result.status("spring.dict.foo1"));


        result = ctx.bucket()
                .mutateIn(key)
                .remove("spring.dict", new SubdocOptionsBuilder().createPath(false).xattr(true))
                .execute();
        assertTrue(result.cas() != 0);
        assertEquals(ResponseStatus.SUCCESS, result.status("spring.dict"));
    }


    @Test
    public void verifyChainDictOpsXATTR() {
        DocumentFragment<Mutation> result = ctx.bucket()
                .mutateIn(key)
                .insert("spring.dict.foo1", "bar1", new SubdocOptionsBuilder().createPath(true).xattr(true))
                .insert("spring.dict.foo2", "bar2", new SubdocOptionsBuilder().createPath(false).xattr(true))
                .upsert("spring.dict.foo1", 0, new SubdocOptionsBuilder().createPath(false).xattr(true))
                .counter("spring.dict.foo1", 1, new SubdocOptionsBuilder().createPath(false).xattr(true))
                .execute();

        assertTrue(result.cas() != 0);
        assertEquals(ResponseStatus.SUCCESS, result.status("spring.dict.foo1"));
        assertEquals(ResponseStatus.SUCCESS, result.status("spring.dict.foo2"));

        result = ctx.bucket()
                .mutateIn(key)
                .remove("spring.dict", new SubdocOptionsBuilder().createPath(false).xattr(true))
                .execute();
        assertTrue(result.cas() != 0);
        assertEquals(ResponseStatus.SUCCESS, result.status("spring.dict"));
    }

    @Test
    public void shouldAllowfullDocGetAndSetWithXattr() {
        String key = "XattrWithFullDoc";
        JsonObject content = JsonObject.create().put("foo", "bar");
        DocumentFragment<Mutation> result = ctx.bucket()
                .mutateIn(key)
                .upsert("spring.class", "SomeClass", new SubdocOptionsBuilder().createPath(true).xattr(true))
                .upsert(content)
                .upsertDocument(true)
                .withExpiry(5)
                .execute(PersistTo.ONE);

        assertTrue(result.cas() != 0);
        assertEquals(ResponseStatus.SUCCESS, result.status(0));
        assertEquals(ResponseStatus.SUCCESS, result.status(1));

        DocumentFragment<Lookup> lookupResult = ctx.bucket()
                .lookupIn(key)
                .get("spring.class", new SubdocOptionsBuilder().xattr(true))
                .get()
                .execute();
        assertTrue(result.cas() != 0);
        assertEquals(ResponseStatus.SUCCESS, lookupResult.status(0));
        assertEquals(ResponseStatus.SUCCESS, lookupResult.status(1));
        assertEquals(content, lookupResult.content(1));

    }

    @Test(expected = CouchbaseException.class)
    public void shouldNotAllowfullDocSetWithXattrWithoutCreateDocument() {
        String key = "XattrWithFullDocFail";
        JsonObject content = JsonObject.create().put("foo", "bar");
        ctx.bucket()
                .mutateIn(key)
                .upsert(content)
                .upsert("spring.class", "SomeClass", new SubdocOptionsBuilder().createPath(true).xattr(true))
                .upsertDocument(false)
                .execute(PersistTo.ONE);
    }

    @Test(expected = CouchbaseException.class)
    public void shouldNotAllowfullDocSetWithXattrWithMissingPath() {
        String key = "XattrWithFullDocMissingPath";
        JsonObject content = JsonObject.create().put("foo", "bar");
        ctx.bucket()
                .mutateIn(key)
                .upsert(content)
                .upsert("spring.class", "SomeClass", new SubdocOptionsBuilder().createPath(false).xattr(true))
                .upsertDocument(false)
                .execute(PersistTo.ONE);
    }

    @Test
    public void shouldAllowfullDocInsertWithXattr() {
        String key = "shouldAllowfullDocInsertWithXattr";
        JsonObject content = JsonObject.create().put("foo", "bar");
        DocumentFragment<Mutation> result = ctx.bucket()
                .mutateIn(key)
                .upsert("spring.class", "SomeClass", new SubdocOptionsBuilder().createPath(false).xattr(true))
                .upsert(content)
                .insertDocument(true)
                .execute(PersistTo.ONE);
        assertEquals(ResponseStatus.SUCCESS, result.status(0));
    }

    @Test(expected = CASMismatchException.class)
    public void shouldFailfullDocInsertWithXattrOnExistingDoc() {
        String key = "shouldFailfullDocInsertWithXattrOnExistingDoc";
        JsonObject content = JsonObject.create().put("foo", "bar");
        DocumentFragment<Mutation> result = ctx.bucket()
                .mutateIn(key)
                .upsert("spring.class", "SomeClass", new SubdocOptionsBuilder().createPath(false).xattr(true))
                .upsert(content)
                .insertDocument(true)
                .execute(PersistTo.ONE);
        assertEquals(ResponseStatus.SUCCESS, result.status(0));
        DocumentFragment<Mutation> upsertResult2 = ctx.bucket()
                .mutateIn(key)
                .upsert("spring.class", "SomeClass2", new SubdocOptionsBuilder().createPath(false).xattr(true))
                .insertDocument(true)
                .execute(PersistTo.ONE);
    }

    @Test
    public void shouldAllowDeletedDocumentXattrLookup() {
        assumeFalse(CouchbaseTestContext.isMockEnabled());

        String key = "shouldAllowDeletedDocumentXattrLookup";
        JsonObject content = JsonObject.create().put("foo", "bar");
        DocumentFragment<Mutation> mutationResult = ctx.bucket()
                .mutateIn(key)
                .upsert("_class", "SomeClass", new SubdocOptionsBuilder().createPath(false).xattr(true))
                .upsert(content)
                .insertDocument(true)
                .execute(PersistTo.ONE);
        assertEquals(ResponseStatus.SUCCESS, mutationResult.status(0));
        ctx.bucket().remove(key);
        DocumentFragment<Lookup> lookupResult = ctx.bucket()
                .lookupIn(key)
                .get("_class", new SubdocOptionsBuilder().xattr(true))
                .accessDeleted(true)
                .execute();
        assertEquals(ResponseStatus.SUCCESS, lookupResult.status(0));
        assertEquals(lookupResult.content(0), "SomeClass");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowBothInsertAndCreateDocument() {
        String key = "shouldNotAllowBothInsertAndCreateDocument";
        JsonObject content = JsonObject.create().put("foo", "bar");
        ctx.bucket()
                .mutateIn(key)
                .upsert("_class", "SomeClass", new SubdocOptionsBuilder().createPath(false).xattr(true))
                .upsert(content)
                .insertDocument(true)
                .upsertDocument(true)
                .execute(PersistTo.ONE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowBothCreateAndInsertDocument() {
        String key = "shouldNotAllowBothCreateAndInsertDocument";
        JsonObject content = JsonObject.create().put("foo", "bar");
        ctx.bucket()
                .mutateIn(key)
                .upsert("_class", "SomeClass", new SubdocOptionsBuilder().createPath(false).xattr(true))
                .upsert(content)
                .upsertDocument(true)
                .insertDocument(true)
                .execute(PersistTo.ONE);
    }

    @Test
    public void shouldExpandMacro() {
        assumeFalse(CouchbaseTestContext.isMockEnabled());

        String key = "shouldExpandMacro";
        JsonObject content = JsonObject.create().put("foo", "bar");
        DocumentFragment<Mutation> mutationResult = ctx.bucket()
                .mutateIn(key)
                .upsert("insertedCas", "${Mutation.CAS}", new SubdocOptionsBuilder()
                        .xattr(true).expandMacros(true))
                .upsert(content)
                .insertDocument(true)
                .execute(PersistTo.ONE);
        assertEquals(ResponseStatus.SUCCESS, mutationResult.status(0));
        DocumentFragment<Lookup> lookupResult = ctx.bucket()
                .lookupIn(key)
                .get("insertedCas", new SubdocOptionsBuilder().xattr(true))
                .execute();
        assertEquals(ResponseStatus.SUCCESS, lookupResult.status(0));
        assertTrue(((String) lookupResult.content(0)).startsWith("0x0000"));
    }

}