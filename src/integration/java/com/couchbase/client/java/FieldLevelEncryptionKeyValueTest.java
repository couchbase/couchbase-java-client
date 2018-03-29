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
import com.couchbase.client.crypto.AES128CryptoProvider;
import com.couchbase.client.crypto.JceksKeyStoreProvider;
import com.couchbase.client.crypto.RSACryptoProvider;
import com.couchbase.client.java.document.EntityDocument;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.crypto.EncryptionConfig;
import com.couchbase.client.java.document.json.ValueEncryptionConfig;
import com.couchbase.client.java.encryption.EncryptionProvider;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.error.InvalidPasswordException;
import com.couchbase.client.java.repository.annotation.EncryptedField;
import com.couchbase.client.java.repository.annotation.Id;
import com.couchbase.client.java.util.TestProperties;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies field level encryptions using AES, RSA using Jceks key store
 */
public class FieldLevelEncryptionKeyValueTest {

    private static Cluster cluster;

    private static Bucket bucket;

    private static RSACryptoProvider rsaCryptoProvider;

    private static AES128CryptoProvider aes128CryptoProvider;

    private ValueEncryptionConfig aesConfig =  new ValueEncryptionConfig(EncryptionProvider.AES128);

    private ValueEncryptionConfig rsaConfig = new ValueEncryptionConfig(EncryptionProvider.RSA);

    @BeforeClass
    public static void setup() throws Exception {
        JceksKeyStoreProvider kp = new JceksKeyStoreProvider("secret");

        String rsaKeyName = "RSAtestkey";
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PublicKey pubKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();
        kp.storeKey(rsaKeyName, pubKey.getEncoded(), privateKey.getEncoded());

        String aesKeyName = "AEStestkey";
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        SecureRandom random = new SecureRandom();
        keyGen.init(128, random);
        SecretKey secretKey = keyGen.generateKey();
        kp.storeKey(aesKeyName, secretKey.getEncoded());

        rsaCryptoProvider = new RSACryptoProvider(rsaKeyName);
        rsaCryptoProvider.setKeyName(rsaKeyName);
        rsaCryptoProvider.setKeyStoreProvider(kp);

        aes128CryptoProvider = new AES128CryptoProvider(kp, aesKeyName);

        aes128CryptoProvider.setKeyName(aesKeyName);
        aes128CryptoProvider.setKeyStoreProvider(kp);

        EncryptionConfig encryptionConfig = new EncryptionConfig();
        encryptionConfig.addCryptoProvider(rsaCryptoProvider);
        encryptionConfig.addCryptoProvider(aes128CryptoProvider);

        CouchbaseEnvironment environment = DefaultCouchbaseEnvironment.builder()
                .encryptionConfig(encryptionConfig)
                .build();
        cluster = CouchbaseCluster.create(environment, TestProperties.seedNode());
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
    public void testStringEncryptAES() {
        JsonDocument document = JsonDocument.create("testStringEncryptAES",
                JsonObject.create().put("foo", "bar", aesConfig));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals("bar", stored.content().get("foo"));
    }

    @Test
    public void testStringEncryptRSA() {
        JsonDocument document = JsonDocument.create("testStringEncryptRSA",
                JsonObject.create().put("foo", "bar", rsaConfig));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals("bar", stored.content().get("foo"));
    }

    @Test
    public void testBooleanEncryptAES() {
        JsonDocument document = JsonDocument.create("testBooleanEncryptAES",
                JsonObject.create().put("foo", true, aesConfig));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(true, stored.content().get("foo"));
    }

    @Test
    public void testBooleanEncryptRSA() {
        JsonDocument document = JsonDocument.create("testBooleanEncryptRSA",
                JsonObject.create().put("foo", true, rsaConfig));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(true, stored.content().get("foo"));
    }

    @Test
    public void testNullEncryptAES() {
        JsonDocument document = JsonDocument.create("testNullEncryptAES",
                JsonObject.create().putNull("foo", aesConfig));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(null, stored.content().get("foo"));
    }

    @Test
    public void testNullEncryptRSA() {
        JsonDocument document = JsonDocument.create("testNullEncryptRSA",
                JsonObject.create().putNull("foo", rsaConfig));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(null, stored.content().get("foo"));
    }

    @Test
    public void testDoubleEncryptAES() {
        JsonDocument document = JsonDocument.create("testDoubleEncryptAES",
                JsonObject.create().put("foo", (double) 1, aesConfig));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals((double) 1, stored.content().get("foo"));
    }

    @Test
    public void testDoubleEncryptRSA() {
        JsonDocument document = JsonDocument.create("testDoubleEncryptRSA",
                JsonObject.create().put("foo", (double) 1, rsaConfig));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals((double) 1, stored.content().get("foo"));
    }

    @Test
    public void testIntEncryptAES() {
        JsonDocument document = JsonDocument.create("testIntEncryptAES",
                JsonObject.create().put("foo", 1, aesConfig));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(1, stored.content().get("foo"));
    }

    @Test
    public void testIntEncryptRSA() {
        JsonObject content =
                JsonObject.create().put("foo", 1, rsaConfig);

        JsonDocument document = JsonDocument.create("testIntEncryptRSA", content);
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(1, stored.content().get("foo"));
    }

    @Test
    public void testLongEncryptAES() {
        JsonDocument document = JsonDocument.create("testLongEncryptAES",
                JsonObject.create().put("foo", 1L, aesConfig));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(1, stored.content().get("foo"));
    }

    @Test
    public void testLongEncryptRSA() {
        JsonDocument document = JsonDocument.create("testLongEncryptRSA",
                JsonObject.create().put("foo", 1L, rsaConfig));
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(1, stored.content().get("foo"));
    }

    @Test
    public void testJsonObjectEncryptAES() {
        JsonObject content = JsonObject.create().put("foo", JsonObject.create().put("bar", "baz"),
                aesConfig);
        JsonDocument document = JsonDocument.create("testJsonObjectEncryptAES",
                content);
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(JsonObject.create().put("bar", "baz"), stored.content().get("foo"));
    }

    @Test
    public void testJsonObjectEncryptRSA() {
        JsonObject content = JsonObject.create().put("foo", JsonObject.create().put("bar", "baz"),
                rsaConfig);
        JsonDocument document = JsonDocument.create("testJsonObjectEncryptRSA",
                content);
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(JsonObject.create().put("bar", "baz"), stored.content().get("foo"));
    }

    @Test
    public void testJsonArrayEncryptAES() {
        JsonObject content = JsonObject.create().put("foo", JsonArray.create().add("bar").add("baz"),
                aesConfig);
        JsonDocument document = JsonDocument.create("testJsonArrayEncryptAES", content);
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(JsonArray.create().add("bar").add("baz"), stored.content().get("foo"));
    }

    @Test
    public void testJsonArrayEncryptRSA() {
        JsonObject content = JsonObject.create().put("foo", JsonArray.create().add("bar").add("baz"),
                rsaConfig);
        JsonDocument document = JsonDocument.create("testJsonArrayEncryptRSA", content);
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertTrue(stored.content().isEncrypted("foo"));
        Assert.assertEquals(JsonArray.create().add("bar").add("baz"), stored.content().get("foo"));
    }

    @Test
    public void testNestedJsonObjectEncryptAES() {
        JsonObject content = JsonObject.create().put("foo1", JsonObject.create().put("foo2",
                JsonObject.create().put("foo3", JsonObject.create().put("foo4", "bar"),
                        aesConfig)));
        JsonDocument document = JsonDocument.create("testNestedJsonObjectEncryptAES",
                content);
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertEquals("{\"foo1\":{\"foo2\":{\"foo3\":{\"foo4\":\"bar\"}}}}", stored.content().toDecryptedString());
    }

    @Test
    public void testNestedJsonObjectEncryptRSA() {
        JsonObject content = JsonObject.create().put("foo1", JsonObject.create().put("foo2",
                JsonObject.create().put("foo3", JsonObject.create().put("foo4", "bar",
                        rsaConfig))));
        JsonDocument document = JsonDocument.create("testNestedJsonObjectEncryptRSA", content);
        bucket.upsert(document);
        JsonDocument stored = bucket.get(document);
        Assert.assertEquals("{\"foo1\":{\"foo2\":{\"foo3\":{\"foo4\":\"bar\"}}}}", stored.content().toDecryptedString());
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
        Assert.assertTrue((entity.stringa.compareTo(stored.content().stringa) == 0) && (entity.stringr.compareTo(stored.content().stringr) == 0));
    }

    public static class Entity {

        @Id
        public String id;

        @EncryptedField(provider = EncryptionProvider.AES128)
        public String stringa;

        @EncryptedField(provider = EncryptionProvider.RSA)
        public String stringr;

        @EncryptedField(provider = EncryptionProvider.AES128)
        public boolean boola;

        @EncryptedField(provider = EncryptionProvider.RSA)
        public boolean boolr;

        @EncryptedField(provider = EncryptionProvider.AES128)
        public long ia;

        @EncryptedField(provider = EncryptionProvider.RSA)
        public long ir;
    }
}