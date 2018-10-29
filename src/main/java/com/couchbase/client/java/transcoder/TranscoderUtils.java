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
package com.couchbase.client.java.transcoder;

import com.couchbase.client.core.endpoint.util.WhitespaceSkipper;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.ByteBufInputStream;
import com.couchbase.client.deps.io.netty.buffer.ByteBufUtil;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.couchbase.client.core.logging.RedactableArgument.user;

/**
 * Helper methods and flags for the shipped {@link Transcoder}s.
 *
 * @author Michael Nitschinger
 * @author Simon Baslé
 * @since 2.0
 */
public class TranscoderUtils {

    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(TranscoderUtils.class);

    /**
     * 32bit flag is composed of:
     *  - 3 compression bits
     *  - 1 bit reserved for future use
     *  - 4 format flags bits. those 8 upper bits make up the common flags
     *  - 8 bits reserved for future use
     *  - 16 bits for legacy flags
     *
     * This mask allows to compare a 32 bits flags with the 4 common flag format bits
     * ("00001111 00000000 00000000 00000000").
     *
     * @see #extractCommonFlags(int)
     * @see #hasCommonFlags(int)
     * @see #hasCompressionFlags(int)
     */
    public static final int COMMON_FORMAT_MASK = 0x0F000000;

    public static final int PRIVATE_COMMON_FLAGS = createCommonFlags(CommonFlags.PRIVATE.ordinal());
    public static final int JSON_COMMON_FLAGS = createCommonFlags(CommonFlags.JSON.ordinal());
    public static final int BINARY_COMMON_FLAGS = createCommonFlags(CommonFlags.BINARY.ordinal());
    public static final int STRING_COMMON_FLAGS = createCommonFlags(CommonFlags.STRING.ordinal());

    public static final int SERIALIZED_LEGACY_FLAGS = 1;
    public static final int BINARY_LEGACY_FLAGS = (8 << 8);
    public static final int STRING_LEGACY_FLAGS = 0;
    public static final int JSON_LEGACY_FLAGS = STRING_LEGACY_FLAGS;
    public static final int BOOLEAN_LEGACY_FLAGS = STRING_LEGACY_FLAGS;
    public static final int LONG_LEGACY_FLAGS = STRING_LEGACY_FLAGS;
    public static final int DOUBLE_LEGACY_FLAGS = STRING_LEGACY_FLAGS;


    public static final int SERIALIZED_COMPAT_FLAGS = PRIVATE_COMMON_FLAGS  | SERIALIZED_LEGACY_FLAGS;
    public static final int JSON_COMPAT_FLAGS       = JSON_COMMON_FLAGS     | JSON_LEGACY_FLAGS;
    public static final int BINARY_COMPAT_FLAGS     = BINARY_COMMON_FLAGS   | BINARY_LEGACY_FLAGS;
    public static final int BOOLEAN_COMPAT_FLAGS    = JSON_COMMON_FLAGS     | BOOLEAN_LEGACY_FLAGS;
    public static final int LONG_COMPAT_FLAGS       = JSON_COMMON_FLAGS     | LONG_LEGACY_FLAGS;
    public static final int DOUBLE_COMPAT_FLAGS     = JSON_COMMON_FLAGS     | DOUBLE_LEGACY_FLAGS;
    public static final int STRING_COMPAT_FLAGS     = STRING_COMMON_FLAGS   | STRING_LEGACY_FLAGS;

    private TranscoderUtils() {}

    /**
     * Checks whether the upper 8 bits are set, indicating common flags presence.
     *
     * It does this by shifting bits to the right until only the most significant
     * bits are remaining and then checks if one of them is set.
     *
     * @param flags the flags to check.
     * @return true if set, false otherwise.
     */
    public static boolean hasCommonFlags(final int flags) {
        return (flags >> 24) > 0;
    }

    /**
     * Checks whether the upper 3 bits are set, indicating compression presence.
     *
     * It does this by shifting bits to the right until only the most significant
     * bits are remaining and then checks if one of them is set.
     *
     * @param flags the flags to check.
     * @return true if compression set, false otherwise.
     */
    public static boolean hasCompressionFlags(final int flags) {
        return (flags >> 29) > 0;
    }

    /**
     * Checks that flags has common flags bits set and that they correspond to expected common flags format.
     *
     * @param flags the 32 bits flags to check
     * @param expectedCommonFlag the expected common flags format bits
     * @return true if common flags bits are set and correspond to expectedCommonFlag format
     */
    public static boolean hasCommonFormat(final int flags,
        final int expectedCommonFlag) {
        return hasCommonFlags(flags) && (flags & COMMON_FORMAT_MASK) == expectedCommonFlag;
    }

    /**
     * Returns only the common flags from the full flags.
     *
     * @param flags the flags to check.
     * @return only the common flags simple representation (8 bits).
     */
    public static int extractCommonFlags(final int flags) {
        return flags >> 24;
    }

    /**
     * Takes a integer representation of flags and moves them to the common flags MSBs.
     *
     * @param flags the flags to shift.
     * @return an integer having the common flags set.
     */
    public static int createCommonFlags(final int flags) {
        return flags << 24;
    }

    /**
     * Utility method to correctly check a flag has a certain type, by checking
     * that either the corresponding flags are set in the common flags bits or
     * the flag is a legacy flag of the correct type.
     *
     * @param flags the flags to be checked.
     * @param expectedCommonFlags the common flags for the expected type
     * @param expectedLegacyFlag the legacy flags for the expected type
     * @return true if flags conform to the correct common flags or legacy flags
     */
    private static boolean hasFlags(final int flags, final int expectedCommonFlags,
        final int expectedLegacyFlag) {
        return hasCommonFormat(flags, expectedCommonFlags) || flags == expectedLegacyFlag;
    }

    /**
     * Checks if the flags identify a JSON document.
     *
     * This method is strict if it finds common flags set, and if not falls back
     * to a check of legacy JSON string (identified by 0 flags and no compression).
     *
     * @param flags the flags to check.
     * @return true if JSON, false otherwise.
     */
    public static boolean hasJsonFlags(final int flags) {
        return hasFlags(flags, JSON_COMMON_FLAGS, JSON_LEGACY_FLAGS);
    }

    /**
     * Checks if the flags identify a String document.
     *
     * This method is strict if it finds common flags set, and if not falls back
     * to a check of legacy String (identified by 0 flags and no compression).
     *
     * @param flags the flags to check.
     * @return true if String, false otherwise.
     */
    public static boolean hasStringFlags(final int flags) {
        return hasFlags(flags, STRING_COMMON_FLAGS, STRING_LEGACY_FLAGS);
    }

    /**
     * Checks if the flags identify a serialized document.
     *
     * @param flags the flags to check.
     * @return true if serializable, false otherwise.
     */
    public static boolean hasSerializableFlags(final int flags) {
        return hasFlags(flags, PRIVATE_COMMON_FLAGS, SERIALIZED_LEGACY_FLAGS);
    }

    /**
     * Checks if the flags identify a binary document.
     *
     * @param flags the flags to check.
     * @return true if binary, false otherwise.
     */
    public static boolean hasBinaryFlags(final int flags) {
        return hasFlags(flags, BINARY_COMMON_FLAGS, BINARY_LEGACY_FLAGS);
    }

    /**
     * Takes the input content and deserializes it.
     *
     * @param content the content to deserialize.
     * @return the serializable object.
     * @throws Exception if something goes wrong during deserialization.
     */
    public static Serializable deserialize(final ByteBuf content) throws Exception {
        byte[] serialized = new byte[content.readableBytes()];
        content.getBytes(0, serialized);
        ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
        ObjectInputStream is = new ObjectInputStream(bis);
        Serializable deserialized = (Serializable) is.readObject();
        is.close();
        bis.close();
        return deserialized;
    }

    /**
     * Serializes the input into a ByteBuf.
     *
     * @param serializable the object to serialize.
     * @return the serialized object.
     * @throws Exception if something goes wrong during serialization.
     */
    public static ByteBuf serialize(final Serializable serializable) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();;
        ObjectOutputStream os = new ObjectOutputStream(bos);
        os.writeObject(serializable);

        byte[] serialized = bos.toByteArray();
        os.close();
        bos.close();
        return Unpooled.buffer().writeBytes(serialized);
    }

    /**
     * Helper method to encode a String into UTF8 via fast-path methods.
     *
     * @param source the source document.
     * @return the encoded byte buffer.
     */
    public static ByteBuf encodeStringAsUtf8(String source) {
        ByteBuf target = Unpooled.buffer(source.length());
        ByteBufUtil.writeUtf8(target, source);
        return target;
    }

    /**
     * A class that holds information from a {@link ByteBuf} that allows to
     * read its corresponding byte array. Offset and length are needed in case
     * the ByteBuf is directly backed by a byte[] but the size of the byte isn't
     * representative of the actual size of the current content.
     */
    public static class ByteBufToArray {
        public final byte[] byteArray;
        public final int offset;
        public final int length;

        public ByteBufToArray(byte[] byteArray, int offset, int length) {
            this.byteArray = byteArray;
            this.offset = offset;
            this.length = length;
        }
    }

    /**
     * Converts a {@link ByteBuf} to a byte[] in the most straightforward manner available.
     * @param input the ByteBuf to convert.
     * @return a {@link ByteBufToArray} containing the byte[] array, as well as the offset and length to use (in case
     * the actual array is longer than the data the ByteBuf represents for instance).
     */
    public static ByteBufToArray byteBufToByteArray(ByteBuf input) {
        byte[] inputBytes;
        int offset = 0;
        int length = input.readableBytes();
        if (input.hasArray()) {
            inputBytes = input.array();
            offset = input.arrayOffset() + input.readerIndex();
        } else {
            inputBytes = new byte[length];
            input.getBytes(input.readerIndex(), inputBytes);
        }
        return new ByteBufToArray(inputBytes, offset, length);
    }

    /**
     * Converts a {@link ByteBuf} to a byte[] in the most straightforward manner available.
     * the byte[] returned is a copy of the actual data
     * @param input the ByteBuf to convert.
     * @return a byte[] array
     */
    public static byte[] copyByteBufToByteArray(ByteBuf input) {
        byte[] copy;
        int length = input.readableBytes();
        if (input.hasArray()) {
            byte[] inputBytes = input.array();
            int offset = input.arrayOffset() + input.readerIndex();
            copy = Arrays.copyOfRange(inputBytes, offset, offset + length);
            return copy;
        } else {
            copy = new byte[length];
            input.getBytes(input.readerIndex(), copy);
        }
        return copy;
    }

    /**
     * Decode a {@link ByteBuf} representing a valid JSON entity to the requested target class,
     * using the {@link ObjectMapper} provided and without releasing the buffer.
     *
     * Mapper uses a {@link ByteBufInputStream} reading directly the netty {@link ByteBuf} if its
     * backed by a direct buffer and avoids a memory copy if backed by a heap buffer by using
     * the original one with an offset.
     *
     * @param input the ByteBuf to decode.
     * @param clazz the class to decode to.
     * @param mapper the mapper to use for decoding.
     * @param <T> the decoded type.
     * @return the decoded value.
     * @throws IOException in case decoding failed.
     */
    public static <T> T byteBufToClass(ByteBuf input, Class<? extends T> clazz, ObjectMapper mapper) throws IOException {
        if (input.hasArray()) {
            ByteBufToArray toArray = byteBufToByteArray(input);
            return mapper.readValue(toArray.byteArray, toArray.offset, toArray.length, clazz);
        } else {
            ByteBufInputStream bbis = null;
            try {
                bbis = new ByteBufInputStream(input);
                return mapper.readValue((InputStream)bbis, clazz);
            }
            finally {
                if (bbis != null) {
                    bbis.close();
                }
            }
        }
    }

    /**
     * Converts a {@link ByteBuf} representing a valid JSON entity to a generic {@link Object},
     * <b>without releasing the buffer</b>. The entity can either be a JSON object, array or scalar value,
     * potentially with leading whitespace (which gets ignored). JSON objects are converted to a {@link JsonObject}
     * and JSON arrays to a {@link JsonArray}.
     *
     * Detection of JSON objects and arrays is attempted in order not to incur an
     * additional conversion step (JSON to Map to JsonObject for example), but if a
     * Map or List is produced, it will be transformed to {@link JsonObject} or
     * {@link JsonArray} (with a warning logged).
     *
     * @param input the buffer to convert. It won't be released.
     * @return a Object decoded from the buffer
     * @throws IOException if the decoding fails.
     */
    public static Object byteBufToGenericObject(ByteBuf input, ObjectMapper mapper) throws IOException {
        //skip leading whitespaces
        int toSkip = input.forEachByte(new WhitespaceSkipper());
        if (toSkip > 0) {
            input.skipBytes(toSkip);
        }
        //peek into the buffer for quick detection of objects and arrays
        input.markReaderIndex();
        byte first = input.readByte();
        input.resetReaderIndex();

        switch (first) {
            case '{':
                return byteBufToClass(input, JsonObject.class, mapper);
            case '[':
                return byteBufToClass(input, JsonArray.class, mapper);
        }

        //we couldn't fast detect the type, we'll have to unmarshall to object and make sure maps and lists
        //are converted to JsonObject/JsonArray.
        Object value = byteBufToClass(input, Object.class, mapper);
        if (value instanceof Map) {
            LOGGER.warn(
              "A JSON object could not be fast detected (first byte '{}')",
              user((char) first)
            );
            return JsonObject.from((Map<String, ?>) value);
        } else if (value instanceof List) {
            LOGGER.warn(
              "A JSON array could not be fast detected (first byte '{}')",
              user((char) first)
            );
            return JsonArray.from((List<?>) value);
        } else {
            return value;
        }
    }

    /**
     * The common flags enum.
     */
    public static enum CommonFlags {
        RESERVED,
        PRIVATE,
        JSON,
        BINARY,
        STRING
    }

}
