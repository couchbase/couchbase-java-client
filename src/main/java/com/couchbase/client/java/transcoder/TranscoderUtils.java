/**
 * Copyright (C) 2014 Couchbase, Inc.
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
package com.couchbase.client.java.transcoder;

import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;

import java.io.*;

/**
 * Helper methods and flags for the shipped {@link Transcoder}s.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class TranscoderUtils {

    public static final int PRIVATE_COMMON_FLAGS = createCommonFlags(CommonFlags.PRIVATE.ordinal());
    public static final int JSON_COMMON_FLAGS = createCommonFlags(CommonFlags.JSON.ordinal());
    public static final int BINARY_COMMON_FLAGS = createCommonFlags(CommonFlags.BINARY.ordinal());
    public static final int STRING_COMMON_FLAGS = createCommonFlags(CommonFlags.STRING.ordinal());

    public static final int SERIALIZED_COMPAT_FLAGS = PRIVATE_COMMON_FLAGS | 1;
    public static final int JSON_COMPAT_FLAGS = JSON_COMMON_FLAGS;
    public static final int BINARY_COMPAT_FLAGS = BINARY_COMMON_FLAGS | (8 << 8);
    public static final int BOOLEAN_COMPAT_FLAGS = JSON_COMMON_FLAGS;
    public static final int LONG_COMPAT_FLAGS = JSON_COMMON_FLAGS;
    public static final int DOUBLE_COMPAT_FLAGS = JSON_COMPAT_FLAGS;
    public static final int STRING_COMPAT_FLAGS = STRING_COMMON_FLAGS;

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
     * Returns only the common flags from the full flags.
     *
     * @param flags the flags to check.
     * @return only the common flags.
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
     * Checks if the flags identify a JSON document.
     *
     * This method is strict if it finds common flags set, and if not falls back
     * to a check of legacy JSON string (identified by 0 flags and no compression).
     *
     * @param flags the flags to check.
     * @return true if JSON, false otherwise.
     */
    public static boolean hasJsonFlags(final int flags) {
        return hasCommonFlags(flags) ? flags == JSON_COMMON_FLAGS : flags == 0;
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
        return hasCommonFlags(flags) ? flags == STRING_COMMON_FLAGS : flags == 0;
    }

    /**
     * Checks if the flags identify a serialized document.
     *
     * @param flags the flags to check.
     * @return true if serializable, false otherwise.
     */
    public static boolean hasSerializableFlags(final int flags) {
        return hasCommonFlags(flags) ? flags == PRIVATE_COMMON_FLAGS : flags == 1;
    }

    /**
     * Checks if the flags identify a binary document.
     *
     * @param flags the flags to check.
     * @return true if binary, false otherwise.
     */
    public static boolean hasBinaryFlags(final int flags) {
        return hasCommonFlags(flags) ? flags == BINARY_COMMON_FLAGS : flags == (8 << 8);
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
