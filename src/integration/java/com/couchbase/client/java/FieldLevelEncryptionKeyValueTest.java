/*
 * Copyright (c) 2018 Couchbase, Inc.
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

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

import com.couchbase.client.encryption.AES128CryptoProvider;
import com.couchbase.client.encryption.CryptoManager;
import com.couchbase.client.encryption.JceksKeyStoreProvider;
import com.couchbase.client.encryption.RSACryptoProvider;
import com.couchbase.client.java.document.EntityDocument;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.error.InvalidPasswordException;
import com.couchbase.client.java.repository.annotation.EncryptedField;
import com.couchbase.client.java.repository.annotation.Id;
import com.couchbase.client.java.util.ClusterDependentTest;
import com.couchbase.client.java.util.CouchbaseTestContext;
import com.couchbase.client.java.util.TestProperties;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.couchbase.client.java.env.DefaultCouchbaseEnvironment.builder;

/**
 * Verifies field level encryptions using AES, RSA using Jceks key store
 */
public class FieldLevelEncryptionKeyValueTest extends ClusterDependentTest {

    private static Cluster cluster;

    private static Bucket bucket;

    private static RSACryptoProvider rsaCryptoProvider;

    private static AES128CryptoProvider aes128CryptoProvider;

    @BeforeClass
    public static void setup() throws Exception {
        JceksKeyStoreProvider kp1 = new JceksKeyStoreProvider("secret");
        JceksKeyStoreProvider kp2 = new JceksKeyStoreProvider("secret");

        String rsaKeyName = "RSAtestkey";
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PublicKey pubKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();
        String rsaPublicKeyName = rsaKeyName + "_public";
        String rsaPrivateKeyName = rsaKeyName + "_private";

        String aesKeyName = "AEStestkey";
        String signingKeyName = "signingKey";

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        SecureRandom random = new SecureRandom();
        keyGen.init(128, random);
        SecretKey secretKey = keyGen.generateKey();
        SecretKey signingKey = keyGen.generateKey();

        kp1.publicKeyName(rsaPublicKeyName);
        kp1.privateKeyName(rsaPrivateKeyName);
        kp1.signingKeyName(rsaPrivateKeyName);
        kp1.storeKey(rsaPublicKeyName, pubKey.getEncoded());
        kp1.storeKey(rsaPrivateKeyName, privateKey.getEncoded());

        kp2.publicKeyName(aesKeyName);
        kp2.signingKeyName(signingKeyName);
        kp2.storeKey(aesKeyName, secretKey.getEncoded());
        kp2.storeKey(signingKeyName, signingKey.getEncoded());

        rsaCryptoProvider = new RSACryptoProvider(kp1);
        aes128CryptoProvider = new AES128CryptoProvider(kp2);

        CryptoManager cryptoManager = new CryptoManager();
        cryptoManager.registerProvider("RSA", rsaCryptoProvider);
        cryptoManager.registerProvider("AES", aes128CryptoProvider);

        DefaultCouchbaseEnvironment.Builder builder = DefaultCouchbaseEnvironment.builder()
                .cryptoManager(cryptoManager);
        if (CouchbaseTestContext.isMockEnabled()) {
            builder
                .bootstrapCarrierDirectPort(ctx.mock.getCarrierPort(ctx.bucketName()))
                .bootstrapHttpDirectPort(ctx.mock.getHttpPort());
        }
        cluster = CouchbaseCluster.create(builder.build(), TestProperties.seedNode());
        try {
            bucket = cluster.openBucket(TestProperties.bucket(), TestProperties.password());
        } catch (InvalidPasswordException ex) {
            // quick hack for RBAC
            cluster.authenticate(TestProperties.adminName(), TestProperties.adminPassword());
            bucket = cluster.openBucket(TestProperties.bucket());
        }
    }

    @AfterClass
    public static void tearDown() {
        cluster.disconnect();
    }

    @Test
    public void testStringEncryptAES() throws Exception {
        JsonDocument document = JsonDocument.create("testStringEncryptAES",
                JsonObject.create().putAndEncrypt("foo", "bar", "AES"));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals("bar", stored.content().getAndDecrypt("foo", "AES"));
    }

    @Test
    public void testStringEncryptRSA() throws Exception {
        JsonDocument document = JsonDocument.create("testStringEncryptRSA",
                JsonObject.create().putAndEncrypt("foo", "bar", "RSA"));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals("bar", stored.content().getAndDecrypt("foo", "RSA"));
    }

    @Test
    public void testBooleanEncryptAES() throws Exception {
        JsonDocument document = JsonDocument.create("testBooleanEncryptAES",
                JsonObject.create().putAndEncrypt("foo", true, "AES"));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(true, stored.content().getAndDecrypt("foo", "AES"));
    }

    @Test
    public void testBooleanEncryptRSA() throws Exception {
        JsonDocument document = JsonDocument.create("testBooleanEncryptRSA",
                JsonObject.create().putAndEncrypt("foo", true, "RSA"));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(true, stored.content().getAndDecrypt("foo", "RSA"));
    }

    @Test
    public void testNullEncryptAES() throws Exception{
        JsonDocument document = JsonDocument.create("testNullEncryptAES",
                JsonObject.create().putNullAndEncrypt("foo", "AES"));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(null, stored.content().getAndDecrypt("foo", "AES"));
    }

    @Test
    public void testNullEncryptRSA() throws Exception {
        JsonDocument document = JsonDocument.create("testNullEncryptRSA",
                JsonObject.create().putNullAndEncrypt("foo", "RSA"));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(null, stored.content().get("foo"));
    }

    @Test
    public void testDoubleEncryptAES() throws Exception {
        JsonDocument document = JsonDocument.create("testDoubleEncryptAES",
                JsonObject.create().putAndEncrypt("foo", (double) 1, "AES"));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals((double) 1, stored.content().getAndDecrypt("foo", "AES"));
    }

    @Test
    public void testDoubleEncryptRSA() throws Exception {
        JsonDocument document = JsonDocument.create("testDoubleEncryptRSA",
                JsonObject.create().putAndEncrypt("foo", (double) 1, "RSA"));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals((double) 1, stored.content().getAndDecrypt("foo", "RSA"));
    }

    @Test
    public void testIntEncryptAES() throws Exception {
        JsonDocument document = JsonDocument.create("testIntEncryptAES",
                JsonObject.create().putAndEncrypt("foo", 1, "AES"));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(1, stored.content().getAndDecrypt("foo", "AES"));
    }

    @Test
    public void testIntEncryptRSA() throws Exception {
        JsonObject content =
                JsonObject.create().putAndEncrypt("foo", 1, "RSA");

        JsonDocument document = JsonDocument.create("testIntEncryptRSA", content);
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(1, stored.content().getAndDecrypt("foo", "RSA"));
    }

    @Test
    public void testLongEncryptAES() throws Exception {
        JsonDocument document = JsonDocument.create("testLongEncryptAES",
                JsonObject.create().putAndEncrypt("foo", 1L, "AES"));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(1, stored.content().getAndDecrypt("foo", "AES"));
    }

    @Test
    public void testLongEncryptRSA() throws Exception {
        JsonDocument document = JsonDocument.create("testLongEncryptRSA",
                JsonObject.create().putAndEncrypt("foo", 1L, "RSA"));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(1, stored.content().getAndDecrypt("foo", "RSA"));
    }

    @Test
    public void testJsonObjectEncryptAES() throws Exception {
        JsonObject content = JsonObject.create().putAndEncrypt("foo", JsonObject.create().put("bar", "baz"),
                "AES");
        JsonDocument document = JsonDocument.create("testJsonObjectEncryptAES",
                content);
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(JsonObject.create().put("bar", "baz"), stored.content().getAndDecrypt("foo", "AES"));
    }

    @Test
    public void testJsonObjectEncryptRSA() throws Exception {
        JsonObject content = JsonObject.create().putAndEncrypt("foo", JsonObject.create().put("bar", "baz"),
                "RSA");
        JsonDocument document = JsonDocument.create("testJsonObjectEncryptRSA",
                content);
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(JsonObject.create().put("bar", "baz"), stored.content().getAndDecrypt("foo", "RSA"));
    }

    @Test
    public void testJsonArrayEncryptAES() throws Exception {
        JsonObject content = JsonObject.create().putAndEncrypt("foo", JsonArray.create().add("bar").add("baz"),
                "AES");
        JsonDocument document = JsonDocument.create("testJsonArrayEncryptAES", content);
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(JsonArray.create().add("bar").add("baz"), stored.content().getAndDecrypt("foo", "AES"));
    }

    @Test
    public void testJsonArrayEncryptRSA() throws Exception {
        JsonObject content = JsonObject.create().putAndEncrypt("foo", JsonArray.create().add("bar").add("baz"),
                "RSA");
        JsonDocument document = JsonDocument.create("testJsonArrayEncryptRSA", content);
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(JsonArray.create().add("bar").add("baz"), stored.content().getAndDecrypt("foo", "RSA"));
    }

    @Test
    public void testNestedJsonObjectEncryptAES() throws Exception {
        JsonObject content = JsonObject.create().put("foo1", JsonObject.create().put("foo2",
                JsonObject.create().putAndEncrypt("foo3", JsonObject.create().put("foo4", "bar"),
                        "AES")));
        JsonDocument document = JsonDocument.create("testNestedJsonObjectEncryptAES",
                content);
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertEquals("{\"foo1\":{\"foo2\":{\"foo3\":{\"foo4\":\"bar\"}}}}", stored.content().toDecryptedString("AES"));
    }

    @Test
    public void testNestedJsonObjectEncryptRSA() throws Exception {
        JsonObject content = JsonObject.create().put("foo1", JsonObject.create().put("foo2",
                JsonObject.create().put("foo3", JsonObject.create().putAndEncrypt("foo4", "bar",
                        "RSA"))));
        JsonDocument document = JsonDocument.create("testNestedJsonObjectEncryptRSA", content);
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertEquals("{\"foo1\":{\"foo2\":{\"foo3\":{\"foo4\":\"bar\"}}}}", stored.content().toDecryptedString("RSA"));
    }

    @Test
    public void testUseWithNoEncryptedFields() {
        JsonDocument document = JsonDocument.create("testUseWithNoEncryptedFields",
                JsonObject.create().put("foo", "bar"));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertEquals("Verifying content", "bar", stored.content().get("foo"));
    }

    @Test
    public void testUseWithNull() {
        JsonDocument document = JsonDocument.create("testUseWithNull", null);
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertEquals("Verifying content", null, stored.content());
    }

    @Test
    public void testEntityEncryption() {
        Entity entity = new Entity();
        entity.id = "testEntityEncryption";
        entity.stringa = "1";
        entity.stringr = "1";
        entity.boola = true;
        entity.boolr = true;
        entity.ia = 1;
        entity.ir = 1;
        EntityDocument<Entity> document = EntityDocument.create(entity);
        bucket.repository().upsert(document);
        EntityDocument<Entity> stored = bucket.repository().get(entity.id, Entity.class);
        Assert.assertTrue((entity.stringa.contentEquals(stored.content().stringa)));
        Assert.assertTrue((entity.stringr.contentEquals(stored.content().stringr)));
        Assert.assertTrue(entity.boola == stored.content().boola);
        Assert.assertTrue(entity.boolr == stored.content().boolr);
        Assert.assertTrue(entity.ia == stored.content().ia);
        Assert.assertTrue(entity.ir == stored.content().ir);
    }

    @Test
    public void testEntityWithNoEncryption() {
        EntityWithNoEncryption entity = new EntityWithNoEncryption();
        entity.id = "testEntityWithNoEncryption";
        entity.content = "foobar";
        EntityDocument<EntityWithNoEncryption> document = EntityDocument.create(entity);
        bucket.repository().upsert(document);
        EntityDocument<EntityWithNoEncryption> stored = bucket.repository().get(entity.id, EntityWithNoEncryption.class);
        Assert.assertEquals("Verifying content", entity.content, stored.content().content);
    }

    public static class Entity {

        @Id
        public String id;

        @EncryptedField(provider = "AES")
        public String stringa;

        @EncryptedField(provider = "RSA")
        public String stringr;

        @EncryptedField(provider = "AES")
        public boolean boola;

        @EncryptedField(provider = "RSA")
        public boolean boolr;

        @EncryptedField(provider = "AES")
        public long ia;

        @EncryptedField(provider = "RSA")
        public long ir;
    }

    public static class EntityWithNoEncryption {
        @Id
        public String id;
        public String content;
    }
}