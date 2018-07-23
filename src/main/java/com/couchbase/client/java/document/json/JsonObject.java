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
package com.couchbase.client.java.document.json;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.encryption.CryptoProvider;
import com.couchbase.client.core.utils.Base64;
import com.couchbase.client.deps.com.fasterxml.jackson.core.JsonProcessingException;
import com.couchbase.client.encryption.CryptoManager;
import com.couchbase.client.java.CouchbaseAsyncBucket;
import com.couchbase.client.java.transcoder.JacksonTransformers;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a JSON object that can be stored and loaded from Couchbase Server.
 *
 * If boxed return values are unboxed, the calling code needs to make sure to handle potential
 * {@link NullPointerException}s.
 *
 * The {@link JsonObject} is backed by a {@link Map} and is intended to work similar to it API wise, but to only
 * allow to store such objects which can be represented by JSON.
 *
 * @author Michael Nitschinger
 * @author Simon Baslé
 * @since 2.0
 */
public class JsonObject extends JsonValue implements Serializable {

    private static final long serialVersionUID = 8817717605659870262L;

    /**
     * The backing {@link Map} for the object.
     */
    private final Map<String, Object> content;

    /**
     * Encryption meta information for the Json values
     */
    private volatile Map<String, String> encryptionPathInfo;

    /**
     * Configuration for decryption, set using the environment
     */
    private volatile CryptoManager cryptoManager;

    /**
     * Encryption prefix
     */
    public static final String ENCRYPTION_PREFIX = "__crypt_";

    /**
     * Private constructor to create the object.
     *
     * The internal map is initialized with the default capacity.
     */
    private JsonObject() {
        content = new HashMap<String, Object>();
    }

    /**
     * Private constructor to create the object with a custom initial capacity.
     */
    private JsonObject(int initialCapacity) {
        content = new HashMap<String, Object>(initialCapacity);
    }

    /**
     * Creates a empty {@link JsonObject}.
     *
     * @return a empty {@link JsonObject}.
     */
    public static JsonObject empty() {
        return new JsonObject();
    }

    /**
     * Creates a empty {@link JsonObject}.
     *
     * @return a empty {@link JsonObject}.
     */
    public static JsonObject create() {
        return new JsonObject();
    }

    /**
     * Constructs a {@link JsonObject} from a {@link Map Map&lt;String, ?&gt;}.
     *
     * This is only possible if the given Map is well formed, that is it contains non null
     * keys, and all values are of a supported type.
     *
     * A null input Map or null key will lead to a {@link NullPointerException} being thrown.
     * If any unsupported value is present in the Map, an {@link IllegalArgumentException}
     * will be thrown.
     *
     * *Sub Maps and Lists*
     * If possible, Maps and Lists contained in mapData will be converted to JsonObject and
     * JsonArray respectively. However, same restrictions apply. Any non-convertible collection
     * will raise a {@link ClassCastException}. If the sub-conversion raises an exception (like an
     * IllegalArgumentException) then it is put as cause for the ClassCastException.
     *
     * @param mapData the Map to convert to a JsonObject
     * @return the resulting JsonObject
     * @throws IllegalArgumentException in case one or more unsupported values are present
     * @throws NullPointerException in case a null map is provided or if it contains a null key
     * @throws ClassCastException if map contains a sub-Map or sub-List not supported (see above)
     */
    public static JsonObject from(Map<String, ?> mapData) {
        if (mapData == null) {
            throw new NullPointerException("Null input Map unsupported");
        } else if (mapData.isEmpty()) {
            return JsonObject.empty();
        }

        JsonObject result = new JsonObject(mapData.size());
        for (Map.Entry<String, ?> entry : mapData.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value == JsonValue.NULL) {
                value = null;
            }

            if (key == null) {
                throw new NullPointerException("The key is not allowed to be null");
            } else if (value instanceof Map) {
                try {
                    JsonObject sub = JsonObject.from((Map<String, ?>) value);
                    result.put(key, sub);
                } catch (ClassCastException e) {
                    throw e;
                } catch (Exception e) {
                    ClassCastException c = new ClassCastException("Couldn't convert sub-Map " + key + " to JsonObject");
                    c.initCause(e);
                    throw c;
                }
            } else if (value instanceof List) {
                try {
                    JsonArray sub = JsonArray.from((List<?>) value);
                    result.put(key, sub);
                } catch (Exception e) {
                    //no risk of a direct ClassCastException here
                    ClassCastException c = new ClassCastException("Couldn't convert sub-List " + key + " to JsonArray");
                    c.initCause(e);
                    throw c;
                }
            } else if (!checkType(value)) {
                throw new IllegalArgumentException("Unsupported type for JsonObject: " + value.getClass());
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * Static method to create a {@link JsonObject} from a JSON {@link String}.
     *
     * The string is expected to be a valid JSON object representation (eg. starting with a '{').
     *
     * @param s the JSON String to convert to a {@link JsonObject}.
     * @return the corresponding {@link JsonObject}.
     * @throws IllegalArgumentException if the conversion cannot be done.
     */
    public static JsonObject fromJson(String s) {
        try {
            return CouchbaseAsyncBucket.JSON_OBJECT_TRANSCODER.stringToJsonObject(s);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot convert string to JsonObject", e);
        }
    }

    /**
     * Stores a {@link Object} value identified by the field name.
     *
     * Note that the value is checked and a {@link IllegalArgumentException} is thrown if not supported.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @return the {@link JsonObject}.
     */
    public JsonObject put(final String name, final Object value) {
        if (this == value) {
            throw new IllegalArgumentException("Cannot put self");
        } else if (value == JsonValue.NULL) {
            putNull(name);
        } else if (checkType(value)) {
            content.put(name, value);
        } else {
            throw new IllegalArgumentException("Unsupported type for JsonObject: " + value.getClass());
        }
        return this;
    }

    /**
     * Stores the {@link Object} value as encrypted identified by the field name.
     *
     * Note that the value is checked and a {@link IllegalArgumentException} is thrown if not supported.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @param providerName Crypto provider name for encryption.
     * @return the {@link JsonObject}.
     */
    public JsonObject putAndEncrypt(final String name, final Object value, String providerName) {
        addValueEncryptionInfo(name, providerName, true);
        if (this == value) {
            throw new IllegalArgumentException("Cannot put self");
        } else if (value == JsonValue.NULL) {
            putNull(name);
        } else if (checkType(value)) {
            content.put(name, value);
        } else {
            throw new IllegalArgumentException("Unsupported type for JsonObject: " + value.getClass());
        }
        return this;
    }

    /**
     * Decrypt value if the name starts with "__encrypt_"
     */
    private Object decrypt(JsonObject object, String providerName) throws Exception {
        Object decrypted;

        String key = object.getString("kid");
        String alg = object.getString("alg");

        CryptoProvider provider = this.cryptoManager.getProvider(providerName);

        if (!provider.checkAlgorithmNameMatch(alg)) {
            cryptoManager.throwMissingPublicKeyEx(providerName);
        }

        if (!key.contentEquals(provider.getKeyStoreProvider().publicKeyName())) {
            cryptoManager.throwMissingPublicKeyEx(providerName);
        }

        byte[] encryptedBytes;
        String encryptedValueWithConfig;

        if (object.containsKey("iv")) {
            encryptedValueWithConfig = object.getString("kid") + object.getString("alg")
                    + object.getString("iv") + object.getString("ciphertext");

            byte[] encrypted = Base64.decode(object.getString("ciphertext"));
            byte[] iv = Base64.decode(object.getString("iv"));
            encryptedBytes = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, encryptedBytes, 0, iv.length);
            System.arraycopy(encrypted, 0, encryptedBytes, iv.length, encrypted.length);
        } else {
            encryptedValueWithConfig = object.getString("kid") + object.getString("alg")
                    + object.getString("ciphertext");
            encryptedBytes = Base64.decode(object.getString("ciphertext"));
        }

        if (object.containsKey("sig")) {
            byte[] signature = Base64.decode(object.getString("sig"));

            if (!provider.verifySignature(encryptedValueWithConfig.getBytes(), signature)) {
                cryptoManager.throwSigningFailedEx(providerName);
            }
        }

        byte[] decryptedBytes = provider.decrypt(encryptedBytes);
        String decryptedString = new String(decryptedBytes, Charset.forName("UTF-8"));
        decrypted = JacksonTransformers.MAPPER.readValue(decryptedString, Object.class);
        if (decrypted instanceof Map) {
            decrypted = JsonObject.from((Map<String, ?>) decrypted);
        } else if (decrypted instanceof List) {
            decrypted = JsonArray.from((List<?>) decrypted);
        }
        return decrypted;
    }

    /**
     * Retrieves the (potential null) content and not casting its type.
     *
     * @param name the key of the field.
     * @return the value of the field, or null if it does not exist.
     */
    public Object get(final String name) {
        return content.get(name);
    }

    /**
     * Retrieve and decrypt content and not casting its type
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name the key of the field
     * @param providerName the crypto algorithm provider name
     * @return the value of the field, or null if it does not exist
     */
    public Object getAndDecrypt(final String name, String providerName) throws Exception {
        return decrypt((JsonObject) content.get(ENCRYPTION_PREFIX + name), providerName);
    }

    /**
     * Stores a {@link String} value identified by the field name.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @return the {@link JsonObject}.
     */
    public JsonObject put(final String name, final String value) {
        content.put(name, value);
        return this;
    }

    /**
     * Stores a {@link String} value as encrypted identified by the field name.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @param providerName the crypto provider name for encryption.
     * @return the {@link JsonObject}.
     */
    public JsonObject putAndEncrypt(final String name, final String value, String providerName) {
        addValueEncryptionInfo(name, providerName, true);
        content.put(name, value);
        return this;
    }

    /**
     * Retrieves the value from the field name and casts it to {@link String}.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name the name of the field.
     * @return the result or null if it does not exist.
     */
    public String getString(String name) {
        return (String) content.get(name);
    }

    /**
     * Retrieves the decrypted value from the field name and casts it to {@link String}.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name the name of the field.
     * @param providerName the crypto provider name for decryption.
     * @return the result or null if it does not exist.
     */
    public String getAndDecryptString(String name, String providerName) throws Exception {
        return (String) getAndDecrypt(name, providerName);
    }

    /**
     * Stores a {@link Integer} value identified by the field name.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @return the {@link JsonObject}.
     */
    public JsonObject put(String name, int value) {
        content.put(name, value);
        return this;
    }

    /**
     * Stores a {@link Integer} value as encrypted identified by the field name.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @param providerName Crypto provider name for encryption.
     * @return the {@link JsonObject}.
     */
    public JsonObject putAndEncrypt(String name, int value, String providerName) {
        addValueEncryptionInfo(name, providerName, true);
        content.put(name, value);
        return this;
    }

    /**
     * Retrieves the value from the field name and casts it to {@link Integer}.
     *
     * Note that if value was stored as another numerical type, some truncation or rounding may occur.
     *
     * @param name the name of the field.
     * @return the result or null if it does not exist.
     */
    public Integer getInt(String name) {
        //let it fail in the more general case where it isn't actually a number
        Number number = (Number) content.get(name);
        if (number == null) {
            return null;
        } else if (number instanceof Integer) {
            return (Integer) number;
        } else {
            return number.intValue(); //autoboxing to Integer
        }
    }

    /**
     * Retrieves the decrypted value from the field name and casts it to {@link Integer}.
     *
     * Note that if value was stored as another numerical type, some truncation or rounding may occur.
     *
     * @param name the name of the field.
     * @param providerName crypto provider name for decryption.
     * @return the result or null if it does not exist.
     */
    @InterfaceStability.Committed
    public Integer getAndDecryptInt(String name, String providerName) throws Exception {
        //let it fail in the more general case where it isn't actually a number
        Number number = (Number) getAndDecrypt(name, providerName);
        if (number == null) {
            return null;
        } else if (number instanceof Integer) {
            return (Integer) number;
        } else {
            return number.intValue(); //autoboxing to Integer
        }
    }

    /**
     * Stores a {@link Long} value identified by the field name.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @return the {@link JsonObject}.
     */
    public JsonObject put(String name, long value) {
        content.put(name, value);
        return this;
    }

    /**
     * Stores a {@link Long} value as encrypted identified by the field name.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @param providerName Crypto provider name for encryption.
     * @return the {@link JsonObject}.
     */
    public JsonObject putAndEncrypt(String name, long value, String providerName) {
        addValueEncryptionInfo(name, providerName, true);
        content.put(name, value);
        return this;
    }

    /**
     * Retrieves the value from the field name and casts it to {@link Long}.
     *
     * Note that if value was stored as another numerical type, some truncation or rounding may occur.
     *
     * @param name the name of the field.
     * @return the result or null if it does not exist.
     */
    public Long getLong(String name) {
        //let it fail in the more general case where it isn't actually a number
        Number number = (Number) content.get(name);
        if (number == null) {
            return null;
        } else if (number instanceof Long) {
            return (Long) number;
        } else {
            return number.longValue(); //autoboxing to Long
        }
    }

    /**
     * Retrieves the decrypted value from the field name and casts it to {@link Long}.
     *
     * Note that if value was stored as another numerical type, some truncation or rounding may occur.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name the name of the field.
     * @param providerName the crypto provider name for decryption
     * @return the result or null if it does not exist.
     */
    public Long getAndDecryptLong(String name, String providerName) throws Exception {
        //let it fail in the more general case where it isn't actually a number
        Number number = (Number) getAndDecrypt(name, providerName);
        if (number == null) {
            return null;
        } else if (number instanceof Long) {
            return (Long) number;
        } else {
            return number.longValue(); //autoboxing to Long
        }
    }

    /**
     * Stores a {@link Double} value identified by the field name.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @return the {@link JsonObject}.
     */
    public JsonObject put(String name, double value) {
        content.put(name, value);
        return this;
    }

    /**
     * Stores a {@link Double} value as encrypted identified by the field name.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @param providerName Crypto provider name for encryption.
     * @return the {@link JsonObject}.
     */
    public JsonObject putAndEncrypt(String name, double value, String providerName) {
        addValueEncryptionInfo(name, providerName, true);
        content.put(name, value);
        return this;
    }

    /**
     * Retrieves the value from the field name and casts it to {@link Double}.
     *
     * Note that if value was stored as another numerical type, some truncation or rounding may occur.
     *
     * @param name the name of the field.
     * @return the result or null if it does not exist.
     */
    public Double getDouble(String name) {
        //let it fail in the more general case where it isn't actually a number
        Number number = (Number) content.get(name);
        if (number == null) {
            return null;
        } else if (number instanceof Double) {
            return (Double) number;
        } else {
            return number.doubleValue(); //autoboxing to Double
        }
    }

    /**
     * Retrieves the value from the field name and casts it to {@link Double}.
     *
     * Note that if value was stored as another numerical type, some truncation or rounding may occur.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name the name of the field.
     * @param providerName the crypto provider name for decryption
     * @return the result or null if it does not exist.
     */
    public Double getAndDecryptDouble(String name, String providerName) throws Exception {
        //let it fail in the more general case where it isn't actually a number
        Number number = (Number) getAndDecrypt(name, providerName);
        if (number == null) {
            return null;
        } else if (number instanceof Double) {
            return (Double) number;
        } else {
            return number.doubleValue(); //autoboxing to Double
        }
    }

    /**
     * Stores a {@link Boolean} value identified by the field name.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @return the {@link JsonObject}.
     */
    public JsonObject put(String name, boolean value) {
        content.put(name, value);
        return this;
    }

    /**
     * Stores a {@link Boolean} value as encrypted identified by the field name.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @param providerName Crypto provider name for encryption.
     * @return the {@link JsonObject}.
     */
    public JsonObject putAndEncrypt(String name, boolean value, String providerName) {
        addValueEncryptionInfo(name, providerName, true);
        content.put(name, value);
        return this;
    }

    /**
     * Retrieves the value from the field name and casts it to {@link Boolean}.
     *
     * @param name the name of the field.
     * @return the result or null if it does not exist.
     */
    public Boolean getBoolean(String name) {
        return (Boolean) content.get(name);
    }

    /**
     * Retrieves the decrypted value from the field name and casts it to {@link Boolean}.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name the name of the field.
     * @param providerName the provider name of the field.
     * @return the result or null if it does not exist.
     */
    public Boolean getAndDecryptBoolean(String name, String providerName) throws Exception {
        return (Boolean) getAndDecrypt(name, providerName);
    }

    /**
     * Stores a {@link JsonObject} value identified by the field name.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @return the {@link JsonObject}.
     */
    public JsonObject put(String name, JsonObject value) {
        if (this == value) {
            throw new IllegalArgumentException("Cannot put self");
        }
        content.put(name, value);
        if (value != null) {
            Map<String, String> paths = value.encryptionPathInfo();
            if (paths != null && !paths.isEmpty()) {
                for (Map.Entry<String, String> entry : paths.entrySet()) {
                    addValueEncryptionInfo(name.replace("~", "~0").replace("/", "~1") + "/" + entry.getKey(), entry.getValue(), false);
                }
                value.clearEncryptionPaths();
            }
        }
        return this;
    }


    /**
     * Stores a {@link JsonObject} value as encrypted identified by the field name.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @param providerName Crypto provider name for encryption.
     * @return the {@link JsonObject}.
     */
    public JsonObject putAndEncrypt(String name, JsonObject value, String providerName) {
        addValueEncryptionInfo(name, providerName, true);
        if (this == value) {
            throw new IllegalArgumentException("Cannot put self");
        }
        content.put(name, value);
        return this;
    }

    /**
     * Attempt to convert a {@link Map} to a {@link JsonObject} value and store it, identified by the field name.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @return the {@link JsonObject}.
     * @see #from(Map)
     */
    public JsonObject put(String name, Map<String, ?> value) {
        return put(name, JsonObject.from(value));
    }

    /**
     * Attempt to convert a {@link Map} to a {@link JsonObject} value and store it,
     * as encrypted identified by the field name.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @param providerName Crypto provider name for encryption.
     * @return the {@link JsonObject}.
     * @see #from(Map)
     */
    public JsonObject putAndEncrypt(String name, Map<String, ?> value, String providerName) {
        addValueEncryptionInfo(name, providerName, true);
        return put(name, JsonObject.from(value));
    }

    /**
     * Retrieves the value from the field name and casts it to {@link JsonObject}.
     *
     * @param name the name of the field.
     * @return the result or null if it does not exist.
     */
    public JsonObject getObject(String name) {
        return (JsonObject) content.get(name);
    }

    /**
     * Retrieves the decrypted value from the field name and casts it to {@link JsonObject}.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name the name of the field.
     * @param providerName Crypto provider name for decryption
     * @return the result or null if it does not exist.
     */
    public JsonObject getAndDecryptObject(String name, String providerName) throws Exception {
        return (JsonObject) getAndDecrypt(name, providerName);
    }

    /**
     * Stores a {@link JsonArray} value identified by the field name.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @return the {@link JsonObject}.
     */
    public JsonObject put(String name, JsonArray value) {
        content.put(name, value);
        return this;
    }

    /**
     * Stores a {@link JsonArray} value as encrypted identified by the field name.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @param providerName the crypto provider name for encryption.
     * @return the {@link JsonObject}.
     */
    public JsonObject putAndEncrypt(String name, JsonArray value, String providerName) {
        addValueEncryptionInfo(name, providerName, true);
        content.put(name, value);
        return this;
    }

    /**
     * Stores a {@link Number} value identified by the field name.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @return the {@link JsonObject}.
     */
    public JsonObject put(String name, Number value) {
        content.put(name, value);
        return this;
    }

    /**
     * Stores a {@link Number} value as encrypted identified by the field name.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @param providerName Crypto provider name for encryption.
     * @return the {@link JsonObject}.
     */
    public JsonObject putAndEncrypt(String name, Number value, String providerName) {
        addValueEncryptionInfo(name, providerName, true);
        content.put(name, value);
        return this;
    }

    /**
     * Stores a {@link JsonArray} value identified by the field name.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @return the {@link JsonObject}.
     */
    public JsonObject put(String name, List<?> value) {
        return put(name, JsonArray.from(value));
    }

    /**
     * Stores a {@link JsonArray} value as encrypted identified by the field name.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name the name of the JSON field.
     * @param value the value of the JSON field.
     * @param providerName the crypto provider name for encryption.
     * @return the {@link JsonObject}.
     */
    public JsonObject putAndEncrypt(String name, List<?> value, String providerName) {
        addValueEncryptionInfo(name, providerName, true);
        return put(name, JsonArray.from(value));
    }

    /**
     * Retrieves the value from the field name and casts it to {@link JsonArray}.
     *
     * @param name the name of the field.
     * @return the result or null if it does not exist.
     */
    public JsonArray getArray(String name) {
        return (JsonArray) content.get(name);
    }

    /**
     * Retrieves the decrypted value from the field name and casts it to {@link JsonArray}.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name the name of the field.
     * @param providerName crypto provider name for decryption.
     * @return the result or null if it does not exist.
     */
    public JsonArray getAndDecryptArray(String name, String providerName) throws Exception {
        return (JsonArray) getAndDecrypt(name, providerName);
    }

    /**
     * Retrieves the value from the field name and casts it to {@link BigInteger}.
     *
     * @param name the name of the field.
     * @return the result or null if it does not exist.
     */
    public BigInteger getBigInteger(String name) {
        return (BigInteger) content.get(name);
    }

    /**
     * Retrieves the decrypted value from the field name and casts it to {@link BigInteger}.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name the name of the field.
     * @param providerName crypto provider name for decryption.
     * @return the result or null if it does not exist.
     */
    public BigInteger getAndDecryptBigInteger(String name, String providerName) throws Exception {
        return (BigInteger) getAndDecrypt(name, providerName);
    }

    /**
     * Retrieves the value from the field name and casts it to {@link BigDecimal}.
     *
     * @param name the name of the field.
     * @return the result or null if it does not exist.
     */
    public BigDecimal getBigDecimal(String name) {
        Object found = content.get(name);
        if (found == null) {
            return null;
        } else if (found instanceof Double) {
            return new BigDecimal((Double) found);
        }
        return (BigDecimal) found;
    }

    /**
     * Retrieves the decrypted value from the field name and casts it to {@link BigDecimal}.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name the name of the field.
     * @param providerName crypto provider for decryption
     * @return the result or null if it does not exist.
     */
    public BigDecimal getAndDecryptBigDecimal(String name, String providerName) throws Exception {
        Object found = getAndDecrypt(name, providerName);
        if (found == null) {
            return null;
        } else if (found instanceof Double) {
            return new BigDecimal((Double) found);
        }
        return (BigDecimal) found;
    }

    /**
     * Retrieves the value from the field name and casts it to {@link Number}.
     *
     * @param name the name of the field.
     * @return the result or null if it does not exist.
     */
    public Number getNumber(String name) {
        return (Number) content.get(name);
    }

    /**
     * Retrieves the decrypted value from the field name and casts it to {@link Number}.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name the name of the field.
     * @param providerName the crypto provider name for decryption
     * @return the result or null if it does not exist.
     */
    public Number getAndDecryptNumber(String name, String providerName) throws Exception {
        return (Number) getAndDecrypt(name, providerName);
    }

    /**
     * Store a null value identified by the field's name.
     *
     * This method is equivalent to calling {@link #put(String, Object)} with either
     * {@link JsonValue#NULL JsonValue.NULL} or a null value explicitly cast to Object.
     *
     * @param name The null field's name.
     * @return the {@link JsonObject}
     */
    public JsonObject putNull(String name) {
        content.put(name, null);
        return this;
    }


    /**
     * Store a null value as encrypted identified by the field's name.
     *
     * This method is equivalent to calling {@link #put(String, Object)} with either
     * {@link JsonValue#NULL JsonValue.NULL} or a null value explicitly cast to Object.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name The null field's name.
     * @param providerName Crypto provider name for encryption.
     * @return the {@link JsonObject}
     */
    public JsonObject putNullAndEncrypt(String name, String providerName) {
        addValueEncryptionInfo(name, providerName, true);
        content.put(name, null);
        return this;
    }

    /**
     * Removes an entry from the {@link JsonObject}.
     *
     * @param name the name of the field to remove
     * @return the {@link JsonObject}
     */
    public JsonObject removeKey(String name) {
        content.remove(name);
        if (this.encryptionPathInfo != null && this.encryptionPathInfo.entrySet().contains(name)) {
            this.encryptionPathInfo.remove(name);
        }
        return this;
    }

    /**
     * Returns a set of field names on the {@link JsonObject}.
     *
     * @return the set of names on the object.
     */
    public Set<String> getNames() {
        return content.keySet();
    }

    /**
     * Returns true if the {@link JsonObject} is empty, false otherwise.
     *
     * @return true if empty, false otherwise.
     */
    public boolean isEmpty() {
        return content.isEmpty();
    }

    /**
     * Transforms the {@link JsonObject} into a {@link Map}. The resulting
     * map is not backed by this {@link JsonObject}, and all sub-objects or
     * sub-arrays ({@link JsonArray}) are also recursively converted to
     * maps and lists, respectively.
     *
     * @return the content copied as a {@link Map}.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> copy = new HashMap<String, Object>(content.size());
        for (Map.Entry<String, Object> entry : content.entrySet()) {
            Object content = entry.getValue();
            if (content instanceof JsonObject) {
                copy.put(entry.getKey(), ((JsonObject) content).toMap());
            } else if (content instanceof JsonArray) {
                copy.put(entry.getKey(), ((JsonArray) content).toList());
            } else {
                copy.put(entry.getKey(), content);
            }
        }
        return copy;
    }

    /**
     * Transforms the {@link JsonObject} into a {@link Map}. The resulting
     * map is not backed by this {@link JsonObject}, and all sub-objects or
     * sub-arrays ({@link JsonArray}) are also recursively converted to
     * maps and lists, respectively. The encrypted values are decrypted.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @return the content copied as a {@link Map}.
     */
    public Map<String, Object> toDecryptedMap(String providerName) throws Exception {
        Map<String, Object> copy = new HashMap<String, Object>(content.size());
        for (Map.Entry<String, Object> entry : content.entrySet()) {
            Object content = entry.getValue();
            String key = entry.getKey();
           if (entry.getKey().startsWith(ENCRYPTION_PREFIX)) {
                content = decrypt((JsonObject) entry.getValue(), providerName);
                key = key.replace(ENCRYPTION_PREFIX, "");
            }
            if (content instanceof JsonObject) {
               JsonObject value = (JsonObject)content;
               value.setCryptoManager(this.cryptoManager);
               copy.put(key,  ((JsonObject) content).toDecryptedMap(providerName));
            } else if (content instanceof JsonArray) {
                copy.put(key, ((JsonArray) content).toList());
            } else {
                copy.put(key, content);
            }
        }
        return copy;
    }

    /**
     * Checks if the {@link JsonObject} contains the field name.
     *
     * @param name the name of the field.
     * @return true if its contained, false otherwise.
     */
    public boolean containsKey(String name) {
        return content.containsKey(name);
    }

    /**
     * Checks if the {@link JsonObject} contains the value.
     *
     * @param value the actual value.
     * @return true if its contained, false otherwise.
     */
    public boolean containsValue(Object value) {
        return content.containsValue(value);
    }

    /**
     * The size of the {@link JsonObject}.
     *
     * @return the size.
     */
    public int size() {
        return content.size();
    }

    /**
     * Returns true if the field is encrypted.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @param name the key name of the field.
     */
    public boolean isEncrypted(final String name) {
        return this.containsKey(ENCRYPTION_PREFIX + name);
    }

    /**
     * Get the encryption list to merge path with parent
     */
    @InterfaceAudience.Private
    public Map<String, String> encryptionPathInfo() {
        return this.encryptionPathInfo;
    }

    /**
     * Clear the encryption paths
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Private
    public void clearEncryptionPaths() {
        if (this.encryptionPathInfo != null) {
            this.encryptionPathInfo.clear();
        }
    }

    /**
     * Set the encryption configuration for decryption
     */
    @InterfaceAudience.Private
    public void setCryptoManager(CryptoManager cryptoManager) {
        this.cryptoManager = cryptoManager;
    }

    /**
     * Get the encryption configuration for decryption
     */
    @InterfaceAudience.Private
    public CryptoManager getCryptoManager() {
        return this.cryptoManager;
    }

    /**
     * Converts the {@link JsonObject} into its JSON string representation.
     *
     * @return the JSON string representing this {@link JsonObject}.
     */
    @Override
    public String toString() {
        try {
            return JacksonTransformers.MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot convert JsonObject to Json String", e);
        }
    }

    /**
     * Converts the {@link JsonObject} into its decrypted JSON string representation.
     *
     * Note: Use of the Field Level Encryption functionality provided in the
     * com.couchbase.client.encryption namespace provided by Couchbase is
     * subject to the Couchbase Inc. Enterprise Subscription License Agreement
     * at https://www.couchbase.com/ESLA-11132015.
     *
     * @return the decrypted JSON string representing this {@link JsonObject}.
     */
    public String toDecryptedString(String providerName) throws Exception {
        Map<String,Object> mapData = this.toDecryptedMap(providerName);
        return JacksonTransformers.MAPPER.writeValueAsString(JsonObject.from(mapData));
    }

    /**
     * Adds to the encryption info with optional escape for json pointer syntax
     *
     * @param path Name of the key
     * @param providerName Value encryption configuration
     */
    @InterfaceAudience.Private
    private void addValueEncryptionInfo(String path, String providerName, boolean escape) {
        if (escape) {
            path = path.replaceAll("~", "~0").replaceAll("/", "~1");
        }
        if (this.encryptionPathInfo == null) {
            this.encryptionPathInfo = new HashMap<String, String>();
        }
        this.encryptionPathInfo.put(path, providerName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JsonObject object = (JsonObject) o;

        if (content != null ? !content.equals(object.content) : object.content != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }
}