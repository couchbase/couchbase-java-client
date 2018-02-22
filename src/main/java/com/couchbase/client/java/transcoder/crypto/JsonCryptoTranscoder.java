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

package com.couchbase.client.java.transcoder.crypto;

import java.util.Map;

import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.encryption.CryptoProvider;
import com.couchbase.client.core.lang.Tuple;
import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.core.utils.Base64;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.core.encryption.EncryptionConfig;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.transcoder.AbstractTranscoder;
import com.couchbase.client.java.transcoder.JacksonTransformers;
import com.couchbase.client.java.transcoder.TranscoderUtils;

/**
 * A transcoder to encode and decode with encryption/decryption support for {@link JsonDocument}s.
 *
 * @author Michael Nitchinger
 * @author Subhashni Balakrishnan
 * @since 2.6.0
 */
@InterfaceStability.Uncommitted
public class JsonCryptoTranscoder extends AbstractTranscoder<JsonDocument, JsonObject> {

    private final EncryptionConfig encryptionConfig;

    public JsonCryptoTranscoder(EncryptionConfig config) {
        this.encryptionConfig = config;
    }

    private void addEncryption(JsonObject content) throws Exception {
        if (content == null || content.encryptionPathInfo().size() == 0) {
            return;
        }

        for (Map.Entry<String, String> entry:content.encryptionPathInfo().entrySet()) {
            String[] pathSplit = entry.getKey().split("/");
            JsonObject parent = content;
            String lastPointer = pathSplit[pathSplit.length-1];

            for (int i=0; i < pathSplit.length-1; i++) {
                parent = (JsonObject) parent.get(pathSplit[i]);
            }

            Object value = parent.get(lastPointer);
            JsonObject encrypted = JsonObject.create();
            CryptoProvider provider = this.encryptionConfig.getCryptoProvider(entry.getValue());
            if (provider == null) {
                throw new Exception("Encryption provider does not exist in the configuration");
            }

            String jsonValue = JacksonTransformers.MAPPER.writeValueAsString(value);

            encrypted.put("kid", provider.getKeyName());
            encrypted.put("alg", entry.getValue());
            encrypted.put("payload", Base64.encode(provider.encrypt(jsonValue.getBytes())));
            parent.removeKey(lastPointer);
            parent.put(JsonObject.ENCRYPTION_PREFIX + lastPointer, encrypted);
        }
        content.clearEncryptionPaths();
    }

    @Override
    public Class<JsonDocument> documentType() {
        return JsonDocument.class;
    }

    @Override
    protected Tuple2<ByteBuf, Integer> doEncode(final JsonDocument document) throws Exception {
        addEncryption(document.content());
        return Tuple.create(jsonObjectToByteBuf(document.content()), TranscoderUtils.JSON_COMPAT_FLAGS);
    }

    @Override
    protected JsonDocument doDecode(String id, ByteBuf content, long cas, int expiry, int flags, ResponseStatus status)
            throws Exception {
        if (!TranscoderUtils.hasJsonFlags(flags)) {
            throw new TranscodingException("Flags (0x" + Integer.toHexString(flags) + ") indicate non-JSON document for "
                    + "id " + id + ", could not decode.");
        }
        JsonObject jsonObject = byteBufToJsonObject(content);
        jsonObject.setEncryptionConfig(encryptionConfig);
        return newDocument(id, expiry, jsonObject, cas);
    }

    @Override
    public JsonDocument newDocument(String id, int expiry, JsonObject content, long cas) {
        JsonDocument document = JsonDocument.create(id, expiry, content, cas);
        document.content().setEncryptionConfig(this.encryptionConfig);
        return document;
    }

    @Override
    public JsonDocument newDocument(String id, int expiry, JsonObject content, long cas,
                                    MutationToken mutationToken) {
        return JsonDocument.create(id, expiry, content, cas, mutationToken);
    }

    public String jsonObjectToString(JsonObject input) throws Exception {
        return JacksonTransformers.MAPPER.writeValueAsString(input);
    }

    private ByteBuf jsonObjectToByteBuf(JsonObject input) throws Exception {
        return Unpooled.wrappedBuffer(JacksonTransformers.MAPPER.writeValueAsBytes(input));
    }

    public JsonObject stringToJsonObject(String input) throws Exception {
        return JacksonTransformers.MAPPER.readValue(input, JsonObject.class);
    }

    /**
     * Converts a {@link ByteBuf} to a {@link JsonObject}, <b>without releasing the buffer</b>
     *
     * @param input the buffer to convert. It won't be cleared (contrary to {@link #doDecode(String, ByteBuf, long, int, int, ResponseStatus) classical decode})
     * @return a JsonObject decoded from the buffer
     * @throws Exception
     */
    public JsonObject byteBufToJsonObject(ByteBuf input) throws Exception {
        return TranscoderUtils.byteBufToClass(input, JsonObject.class, JacksonTransformers.MAPPER);
    }

    /**
     * Converts a {@link ByteBuf} representing a valid JSON entity to a generic {@link Object},
     * <b>without releasing the buffer</b>. The entity can either be a JSON object, array or scalar value, potentially with leading whitespace (which gets ignored).
     *
     * Detection of JSON objects and arrays is attempted in order not to incur an
     * additional conversion step (JSON to Map to JsonObject for example), but if a
     * Map or List is produced, it will be transformed to {@link JsonObject} or
     * {@link JsonArray} (with a warning logged).
     *
     * @param input the buffer to convert. It won't be cleared (contrary to
     * {@link #doDecode(String, ByteBuf, long, int, int, ResponseStatus) classical decode})
     * @return a Object decoded from the buffer
     * @throws Exception
     */
    public Object byteBufJsonValueToObject(ByteBuf input) throws Exception {
        return TranscoderUtils.byteBufToGenericObject(input, JacksonTransformers.MAPPER);
    }
}