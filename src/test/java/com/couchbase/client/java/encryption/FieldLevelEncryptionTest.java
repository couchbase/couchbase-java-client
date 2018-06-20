package com.couchbase.client.java.encryption;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import com.couchbase.client.encryption.AES256CryptoProvider;
import com.couchbase.client.encryption.CryptoManager;
import com.couchbase.client.encryption.JceksKeyStoreProvider;
import com.couchbase.client.encryption.RSACryptoProvider;
import com.couchbase.client.java.document.json.JsonObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies the decryption of the encrypted examples given by the RFC 0032
 */
public class FieldLevelEncryptionTest {

    private static CryptoManager cryptoManager;

    @BeforeClass
    public static void setup() throws Exception {
        JceksKeyStoreProvider kp1 = new JceksKeyStoreProvider("secret");
        kp1.storeKey("mypublickey", "!mysecretkey#9^5usdk39d&dlf)03sL".getBytes(Charset.forName("UTF-8")));
        kp1.storeKey("HMACsecret",  "myauthpassword".getBytes(Charset.forName("UTF-8")));
        kp1.signingKeyName("HMACsecret");
        kp1.publicKeyName("mypublickey");

        AES256CryptoProvider aes256CryptoProvider = new AES256CryptoProvider(kp1);

        String privateKeyStr = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDA/qz+yKr6B5kB" +
                                "w3znxqzVTpUgV40OO96mXzyPJ3vjyJ0O3VqEOrDukbDlYzKCrThqdZAbeQpRSESX" +
                                "HAb0En/PivElc7RVt+W/TCGAGVNVXFe5dI/C3IDbTQIQ0s7ppNpU39p621K8DdwS" +
                                "5OK3p6DgDKLSJI73tIj/0mnhd4jhsDkuznnVj1dH49Vudo2ANbEa95unD0a+N4r+" +
                                "FR0wfA/xCEt3IsuNtDqLdjE5YVhaqO1JBq8J2Skc26LtTWtQvFhgVICFHrh07ChO" +
                                "zbIHyUsxCyNN0hBHqhBmBopGVXM/MeUmCnS0hInG8lhety3tkSMQW1Muc4zHgTsx" +
                                "uZw3duwPAgMBAAECggEAaKKrkIeji2PLJRWkBtXEpvGwEJTnOSxkjrdb0hGKLfl6" +
                                "jbCdfsuDWhVLX1Lk88yOpcmPlBWP7nnMFlFvw6yz9wZRsAiHYWIPAiR4lUcl00X5" +
                                "mecEepWqlzutPwnMfQiQByxG/A0lUigBhYzrDr+njVHMhTqk+M+851ZhaYixggpk" +
                                "CFWdN/xBcMGkF2POuuVOehwKE20R2SrIIWYUgs5ZSIVvNawcl7n/Wj6/4rDipAYc" +
                                "1lWkWLa9AInFMt0RtxQcKZWqBA6z7WGz4DHfumRd9UFGH3sTlFXQ6zwm0feybD24" +
                                "JxC94lqea49WGxr7zOM4raPUAeN2xo9mAw/C6V5WgQKBgQDmYnvaU9FwWM1NMNB8" +
                                "tQfEKritZ6SRL5CyA/IG4TzPmaLtKwe5JxWL49wt6FFjx7pnjrgqX02LpzXsdwz4" +
                                "GbxrLvSHE95OWqtUGPEqJOLEXUFxxsEc+N0mXRYIQ4PEoynlMBqwmx8zs+DXxxg1" +
                                "qSY6or9qBzssm2zKj32Mp8VhHwKBgQDWc/OYZ+FlnYlew0E2WrK+K1XtTPBrrYMw" +
                                "YlS6iw4OdrPm/iBM9mEwXiBo7CHfCh8/zzYlkzVgQMXrg2fe0wNciaJ2gT20oMw/" +
                                "JPX6htoEQ97wB+EAkZ8ytYeBTdiJx3XpvEq9tTeQ4OkWAG2cTnQlxx4TIVhNbNl4" +
                                "1FaWxbhnEQKBgQCpPEMa2GOLkdAOGgOs+BaiZXeP+giLllNGUVui7iYLoiJq8icU" +
                                "Pb+4KUP+fR/8miU2GULz7Vo7cjNMZw+h2NXuLmn2KAQvrq8YcdIGUV47PP3sJEKL" +
                                "k8xweATNQTs0YV9POo0AmpLLGiHaoCgKkxzACfluW61+URYTnmBtyHhXpQKBgQCs" +
                                "WIFTYWDOZl3o72hwQ1HU7UTgMe4hy09cShon1OsWCqWoJWFWGMegtHS9fc/2zM6y" +
                                "XFf6uKSz1zp4fKG0fMb9zornTBSIHpYmxRB+J3P864K2Ss6zw1Q6z5K4AxTcHZWQ" +
                                "o8c5UPL4Fxibmvp8HLzRQ4XTAABUMP9RUOzJvNrm0QKBgH6SxmJRtn7xZNdUgHcc" +
                                "klPxTxtnzrvqWtFQnljEH5q8eHiD9Mj57neNkoFW8HcyGnpAUxkhO5RNGCIa4CMF" +
                                "qnhJnfY+bK7dPGJYPEqS2a8Uw8tRUWe0MnS2Ha71enDz5PTUA4IVzQ8q0LyDQKi/" +
                                "G76pYt+qcmdMLH5DTf51CP2t";

        String pubKeyStr = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwP6s/siq+geZAcN858as1U6VIFeNDj" +
                "vepl88jyd748idDt1ahDqw7pGw5WMygq04anWQG3kKUUhElxwG9BJ/z4rxJXO0Vbflv0whgBlTVVxXuXSPwty" +
                "A200CENLO6aTaVN/aettSvA3cEuTit6eg4Ayi0iSO97SI/9Jp4XeI4bA5Ls551Y9XR+PVbnaNgDWxGvebpw9Gv" +
                "jeK/hUdMHwP8QhLdyLLjbQ6i3YxOWFYWqjtSQavCdkpHNui7U1rULxYYFSAhR64dOwoTs2yB8lLMQsjTdIQR6oQZgaKRlVzPzHlJgp0tISJxvJYXrct7ZEjEFtTLnOMx4E7MbmcN3bsDwIDAQAB";

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        X509EncodedKeySpec spec = new X509EncodedKeySpec(DatatypeConverter.parseBase64Binary(pubKeyStr));
        PublicKey publicKey = keyFactory.generatePublic(spec);

        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(DatatypeConverter.parseBase64Binary(privateKeyStr));
        PrivateKey privateKey = keyFactory.generatePrivate(privSpec);

        JceksKeyStoreProvider kp2 = new JceksKeyStoreProvider("secret");
        kp2.privateKeyName("MyPrivateKeyName");
        kp2.publicKeyName("MyPublicKeyName");
        kp2.storeKey("MyPrivateKeyName", privateKey.getEncoded());
        kp2.storeKey("MyPublicKeyName", publicKey.getEncoded());

        RSACryptoProvider rsaCryptoProvider = new RSACryptoProvider(kp2);

        cryptoManager = new CryptoManager();
        cryptoManager.registerProvider("AES", aes256CryptoProvider);
        cryptoManager.registerProvider("RSA", rsaCryptoProvider);
    }

    @Test
    public void testEncryptString() throws Exception {
        JsonObject jsonObject = JsonObject.create();
        JsonObject encrypted = JsonObject.create();
        encrypted.put("alg", "AES-256-HMAC-SHA256");
        encrypted.put("kid", "mypublickey");
        encrypted.put("iv", "Cfq84/46Qjet3EEQ1HUwSg==");
        encrypted.put("ciphertext", "sR6AFEIGWS5Fy9QObNOhbCgfg3vXH4NHVRK1qkhKLQqjkByg2n69lot89qFEJuBsVNTXR77PZR6RjN4h4M9evg==");
        encrypted.put("sig", "rT89aCj1WosYjWHHu0mf92S195vYnEGA/reDnYelQsM=");
        jsonObject.put("__crypt_message" , encrypted);
        jsonObject.setCryptoManager(cryptoManager);
        Assert.assertEquals(jsonObject.toDecryptedMap("AES").get("message").toString(), "The old grey goose jumped over the wrickety gate.");
    }

    @Test
    public void testEncryptNumber() throws Exception {
        JsonObject jsonObject = JsonObject.create();
        JsonObject encrypted = JsonObject.create();
        encrypted.put("alg", "AES-256-HMAC-SHA256");
        encrypted.put("kid", "mypublickey");
        encrypted.put("iv", "wAg/Z+c81em+to/rR9T3PA==");
        encrypted.put("ciphertext", "bvfUk9qkfCYKS2S5CCJPpg==");
        encrypted.put("sig", "LAcwxznVSED4zQbuy+UjacQlvtVYvpVmiiAU5gJJASc=");
        jsonObject.put("__crypt_message" , encrypted);
        jsonObject.setCryptoManager(cryptoManager);
        Assert.assertEquals(jsonObject.toDecryptedMap("AES").get("message"), 10);
    }

    @Test
    public void testEncryptNumberString() throws Exception {
        JsonObject jsonObject = JsonObject.create();
        JsonObject encrypted = JsonObject.create();
        encrypted.put("alg", "AES-256-HMAC-SHA256");
        encrypted.put("kid", "mypublickey");
        encrypted.put("iv", "jdqfaa9Hjpd5rTi2BaEWWg==");
        encrypted.put("ciphertext", "r6rK6mO0KQ1p9ws/8Feqyg==");
        encrypted.put("sig", "NdvUTdR6XhRZQnQBWVzZjv9MxNIbzmwslQqP6onNdVk=");
        jsonObject.put("__crypt_message" , encrypted);
        jsonObject.setCryptoManager(cryptoManager);
        Assert.assertEquals(jsonObject.toDecryptedMap("AES").get("message").toString(), "10");
    }

    @Test
    public void testEncryptArray() throws Exception {
        JsonObject jsonObject = JsonObject.create();
        JsonObject encrypted = JsonObject.create();
        encrypted.put("alg", "AES-256-HMAC-SHA256");
        encrypted.put("kid", "mypublickey");
        encrypted.put("iv", "A4OSMlz95cvn6ZDypm58jA==");
        encrypted.put("ciphertext", "9aTRYMmbNf6tvVFpbedSsS5Hdhk/OjUIz2mEqp5L5EcVNGoKJBhnuaAu35fNVM2YW/7TscXdiUBaeZZv7Zxg1Zve+A1u1/7dmgbkvAilNSo=");
        encrypted.put("sig", "PQ25q0k271CpZ9quOg2m3oAIZVa6Mh9S0mo15nN/hlk=");
        jsonObject.put("__crypt_message" , encrypted);
        jsonObject.setCryptoManager(cryptoManager);
        Assert.assertEquals(jsonObject.toDecryptedMap("AES").get("message"), Arrays.asList("The","Old","Grey","Goose","Jumped","over","the","wrickety","gate"));
    }

    @Test
    public void testEncryptObject() throws Exception {
        JsonObject jsonObject = JsonObject.create();
        JsonObject encrypted = JsonObject.create();
        encrypted.put("alg", "AES-256-HMAC-SHA256");
        encrypted.put("kid", "mypublickey");
        encrypted.put("iv", "kKz59c/ObctWVBVen7VqHw==");
        encrypted.put("ciphertext", "LIoZ3qPbEAmNbUUsJeRngN2tDo8/gU1AQ+yY88sgcdACMz/DjD8+kAzbZHYiWswGSlDaEHXs9c2hIYiD2/AkX/qfaGcAZLt4rjZpbzuNf+Q=");
        encrypted.put("sig", "inEyX1OYv2VbO60o16zId8eacwE8E1KrKcfgQVlz4kA=");
        jsonObject.put("__crypt_message" , encrypted);
        jsonObject.setCryptoManager(cryptoManager);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("myValue","The old grey goose jumped over the wrickety gate.");
        map.put("myInt", 10);
        Assert.assertEquals(jsonObject.toDecryptedMap("AES").get("message"), map);
    }

    @Test
    public void testEncryptStringUsingRSA() throws Exception {
        JsonObject jsonObject = JsonObject.create();
        JsonObject encrypted = JsonObject.create();
        encrypted.put("alg", "RSA-2048-OAEP-SHA1");
        encrypted.put("kid", "MyPublicKeyName");
        encrypted.put("ciphertext", "JMAfWsDrRdrF/ghjq4xlysN7F6TogIWhkf8Fa6Qvfhqq+syKXmtviIMVSBOXPJvZm8gT/wyryjqBrLFPK9AeeS2mI4FsPCbZzvRS85f12GS0TNIp71wW1R2BNFt+51Oa5jD1SOGT/qrBbpFWICVh2z+8AUbxirrobAFn3L179sAmcj8mP3Hl0+4YeTMx/DI333bsGvpB7bpk4U28/38PMhR8Uc1xjXpW1NUwfqho4PUPMumDPUa5e8p2DMUaGzgl6PVZmX3xqHGpBviWqcKGORQhOsWfI/45IDaWJlk7eHv8yuvJleKKd1cUkkRfZ3R1bui2F9S/HvJdQKClfHc3Ow==");
        jsonObject.put("__crypt_message" , encrypted);
        jsonObject.setCryptoManager(cryptoManager);
        Assert.assertEquals(jsonObject.toDecryptedMap("RSA").get("message"), "The old grey goose jumped over the wrickety gate.");
    }

    @Test
    public void testEncryptIntegerUsingRSA() throws Exception {
        JsonObject jsonObject = JsonObject.create();
        JsonObject encrypted = JsonObject.create();
        encrypted.put("alg", "RSA-2048-OAEP-SHA1");
        encrypted.put("kid", "MyPublicKeyName");
        encrypted.put("ciphertext", "NTxRjexminStAPC2fv6jXbaLPk4wxOkI8xU4EgmR05qZzmfjN+U1e5Ipi9mC/dOk0RfFklS+30ss45GkpZZnYjlEKQ9gI4ZmTb5Za42SHB6hhrR1ZXCIGfH4UhUwQiM+XzHLvOOiTnxVcSQd3TgL/hlTUbUsIWsqrm5Q9O1R+h8suEjcOnu4mmRI1qMdQLSKXPtqa8N1u00F24QNtS79UeWaZFVqll7FyESyJaz86ZS1/0NXwkfCwPRD0iP7Q/mfKh5+Vtl22PM9k1ar3aHbkJhE11Pm5y7w0Z9K1X73CmcSWYBuOY/SDpIBmqLYtv4o1ANB+bMv7yo+uoCouFrD/A==");
        jsonObject.put("__crypt_message" , encrypted);
        jsonObject.setCryptoManager(cryptoManager);
        Assert.assertEquals(jsonObject.toDecryptedMap("RSA").get("message"), 10);
    }

    @Test
    public void testEncryptArrayUsingRSA() throws Exception {
        JsonObject jsonObject = JsonObject.create();
        JsonObject encrypted = JsonObject.create();
        encrypted.put("alg", "RSA-2048-OAEP-SHA1");
        encrypted.put("kid", "MyPublicKeyName");
        encrypted.put("ciphertext", "KlYYMyaJRZvNr+tyoK5E75lE+QWSrsvmraBoapl/l9RRHkjien7+AqcmVsS/dRRa9Ad8dmyRvaOA9B46TsJ2FbzNJ8cVNTyLPdAeluU9aM0IiIuMfEYFc5XNC2clpQlsVgxMutiO0wiCEvFX3iNIvZFeYUQofKoe0H1VlyGGZbfLLdNfl+Rlui0IULFTW6UZEnmiIlTxffnGdvlwWlaTpJMfTAIYOieZiPbsraGgjIpPrQtXVSyy/bSqmOp2eva+X7dtD7R3vAHlRptvD4Muhp3jaxIQj0J4NsD4Gw+muHFYG1YnsdJERyWlkMQmnJt89XPL2VUD6ni2Q8TyxFm0LA==");
        jsonObject.put("__crypt_message" , encrypted);
        jsonObject.setCryptoManager(cryptoManager);
        Assert.assertEquals(jsonObject.toDecryptedMap("RSA").get("message"), Arrays.asList("The", "Old", "Grey","Goose","Jumped","over","the","wrickety","gate"));
    }

    @Test
    public void testEncryptObjectUsingRSA() throws Exception {
        JsonObject jsonObject = JsonObject.create();
        JsonObject encrypted = JsonObject.create();
        encrypted.put("alg", "RSA-2048-OAEP-SHA1");
        encrypted.put("kid", "MyPublicKeyName");
        encrypted.put("ciphertext", "E2Tlzl6MFTYnnKip7ENdZL8NuA/XkWPllWXu/nws4lYKHxVg8A1XYo+Q229q145glk73S01QmHbB0CZXbzvTo/BgZBb3but1U97qsoPnajFo6BVvigpCt6gaZnYSHuoXsB4L/JBRJuw+0cLX5sr7PsRb5WTV3eTCo/ja2jnqUSOCbWmwasqBY49dvSuJfTwWLcgOWJeg58AZoAGZEqAkuavoxD/+vtRXbFLXO3qdQ4XhsJjnttnaQnjgpKJOzYYwhpF7U0pW0YzT7PvtbJdguMeiYwd0Ypt5L7WiLr89ft0RFDO6K+x66fnxk4hM1c/xeCOAlNR/Mu75ke1/QZpnGg==");
        jsonObject.put("__crypt_message" , encrypted);
        jsonObject.setCryptoManager(cryptoManager);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("myValue","The old grey goose jumped over the wrickety gate.");
        map.put("myInt", 10);
        Assert.assertEquals(jsonObject.toDecryptedMap("RSA").get("message"), map);
    }
}