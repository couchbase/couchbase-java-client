package com.couchbase.client.java.encryption;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.couchbase.client.crypto.AES256CryptoProvider;
import com.couchbase.client.crypto.EncryptionConfig;
import com.couchbase.client.crypto.JceksKeyStoreProvider;
import com.couchbase.client.java.document.json.JsonObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies the decryption of the encrypted examples given by the RFC 0032
 */
public class FieldLevelEncryptionTest {

    private static EncryptionConfig encryptionConfig;

    @BeforeClass
    public static void setup() throws Exception {
        JceksKeyStoreProvider kp = new JceksKeyStoreProvider("secret");
        String aesKeyName = "mypublickey";
        byte[] secret = "!mysecretkey#9^5usdk39d&dlf)03sL".getBytes(Charset.forName("UTF-8"));
        kp.storeKey(aesKeyName, secret);

        String hmacKeyName = "HMACsecret";
        secret = "myauthpassword".getBytes(Charset.forName("UTF-8"));
        kp.storeKey(hmacKeyName, secret);

        AES256CryptoProvider aes256CryptoProvider = new AES256CryptoProvider(kp, aesKeyName, hmacKeyName);
        encryptionConfig = new EncryptionConfig();
        encryptionConfig.addCryptoProvider(aes256CryptoProvider);
    }

    @Test
    public void testEncryptString() {
        JsonObject jsonObject = JsonObject.create();
        JsonObject encrypted = JsonObject.create();
        encrypted.put("alg", "AES-256-HMAC-SHA256");
        encrypted.put("kid", "mypublickey");
        encrypted.put("iv", "Cfq84/46Qjet3EEQ1HUwSg==");
        encrypted.put("ciphertext", "sR6AFEIGWS5Fy9QObNOhbCgfg3vXH4NHVRK1qkhKLQqjkByg2n69lot89qFEJuBsVNTXR77PZR6RjN4h4M9evg==");
        encrypted.put("sig", "rT89aCj1WosYjWHHu0mf92S195vYnEGA/reDnYelQsM=");
        jsonObject.put("__crypt_message" , encrypted);
        jsonObject.setEncryptionConfig(encryptionConfig);
        Assert.assertEquals(jsonObject.toDecryptedMap().get("message").toString(), "The old grey goose jumped over the wrickety gate.");
    }

    @Test
    public void testEncryptNumber() {
        JsonObject jsonObject = JsonObject.create();
        JsonObject encrypted = JsonObject.create();
        encrypted.put("alg", "AES-256-HMAC-SHA256");
        encrypted.put("kid", "mypublickey");
        encrypted.put("iv", "wAg/Z+c81em+to/rR9T3PA==");
        encrypted.put("ciphertext", "bvfUk9qkfCYKS2S5CCJPpg==");
        encrypted.put("sig", "LAcwxznVSED4zQbuy+UjacQlvtVYvpVmiiAU5gJJASc=");
        jsonObject.put("__crypt_message" , encrypted);
        jsonObject.setEncryptionConfig(encryptionConfig);
        Assert.assertEquals(jsonObject.toDecryptedMap().get("message"), 10);
    }

    @Test
    public void testEncryptNumberString() {
        JsonObject jsonObject = JsonObject.create();
        JsonObject encrypted = JsonObject.create();
        encrypted.put("alg", "AES-256-HMAC-SHA256");
        encrypted.put("kid", "mypublickey");
        encrypted.put("iv", "jdqfaa9Hjpd5rTi2BaEWWg==");
        encrypted.put("ciphertext", "r6rK6mO0KQ1p9ws/8Feqyg==");
        encrypted.put("sig", "NdvUTdR6XhRZQnQBWVzZjv9MxNIbzmwslQqP6onNdVk=");
        jsonObject.put("__crypt_message" , encrypted);
        jsonObject.setEncryptionConfig(encryptionConfig);
        Assert.assertEquals(jsonObject.toDecryptedMap().get("message").toString(), "10");
    }

    @Test
    public void testEncryptArray() {
        JsonObject jsonObject = JsonObject.create();
        JsonObject encrypted = JsonObject.create();
        encrypted.put("alg", "AES-256-HMAC-SHA256");
        encrypted.put("kid", "mypublickey");
        encrypted.put("iv", "A4OSMlz95cvn6ZDypm58jA==");
        encrypted.put("ciphertext", "9aTRYMmbNf6tvVFpbedSsS5Hdhk/OjUIz2mEqp5L5EcVNGoKJBhnuaAu35fNVM2YW/7TscXdiUBaeZZv7Zxg1Zve+A1u1/7dmgbkvAilNSo=");
        encrypted.put("sig", "PQ25q0k271CpZ9quOg2m3oAIZVa6Mh9S0mo15nN/hlk=");
        jsonObject.put("__crypt_message" , encrypted);
        jsonObject.setEncryptionConfig(encryptionConfig);
        Assert.assertEquals(jsonObject.toDecryptedMap().get("message"), Arrays.asList("The","Old","Grey","Goose","Jumped","over","the","wrickety","gate"));
    }

    @Test
    public void testEncryptObject() {
        JsonObject jsonObject = JsonObject.create();
        JsonObject encrypted = JsonObject.create();
        encrypted.put("alg", "AES-256-HMAC-SHA256");
        encrypted.put("kid", "mypublickey");
        encrypted.put("iv", "kKz59c/ObctWVBVen7VqHw==");
        encrypted.put("ciphertext", "LIoZ3qPbEAmNbUUsJeRngN2tDo8/gU1AQ+yY88sgcdACMz/DjD8+kAzbZHYiWswGSlDaEHXs9c2hIYiD2/AkX/qfaGcAZLt4rjZpbzuNf+Q=");
        encrypted.put("sig", "inEyX1OYv2VbO60o16zId8eacwE8E1KrKcfgQVlz4kA=");
        jsonObject.put("__crypt_message" , encrypted);
        jsonObject.setEncryptionConfig(encryptionConfig);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("myValue","The old grey goose jumped over the wrickety gate.");
        map.put("myInt", 10);
        Assert.assertEquals(jsonObject.toDecryptedMap().get("message"), map);
    }
}