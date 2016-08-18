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
package com.couchbase.client.java.transcoder.subdoc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.ByteBufOutputStream;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.subdoc.MultiValue;
import com.couchbase.client.java.transcoder.TranscoderUtils;

/**
 * An {@link AbstractFragmentTranscoder} that further implements decoding and encoding of messaging,
 * easing the implementation of a concrete {@link FragmentTranscoder} based on byte arrays zero-copied
 * from {@link ByteBuf}.
 *
 * Note that the serialization/deserialization mean should be able to work with byte arrays and write to
 * an {@link OutputStream}, and that byte arrays should be treated as transient (eg. not used to back long
 * living objects), as they may be tied to the original {@link ByteBuf} which will get released.
 *
 * @author Simon Baslé
 * @since 2.3
 */
public abstract class AbstractByteArrayFragmentTranscoder extends AbstractFragmentTranscoder {

    @Override
    public <T> T decodeWithMessage(ByteBuf encoded, Class<? extends T> clazz, String transcodingErrorMessage) throws TranscodingException {
        try {
            TranscoderUtils.ByteBufToArray toArray = TranscoderUtils.byteBufToByteArray(encoded);
            if (Object.class.equals(clazz)) {
                //generic path that will transform dictionaries to JsonObject and arrays to JsonArray
                return (T) byteArrayToGenericObject(toArray.byteArray, toArray.offset, toArray.length);
            } else {
                return byteArrayToClass(toArray.byteArray, toArray.offset, toArray.length, clazz);
            }
        } catch (Exception e) {
            throw new TranscodingException(transcodingErrorMessage, e);
        }
    }

    @Override
    protected <T> ByteBuf doEncodeSingle(T value, String transcodingErrorMessage) throws TranscodingException {
        try {
            return Unpooled.wrappedBuffer(writeValueAsBytes(value));
        } catch (Exception e) {
            throw new TranscodingException(transcodingErrorMessage, e);
        }
    }

    @Override
    protected ByteBuf doEncodeMulti(MultiValue<?> multiValue, String transcodingErrorMessage) throws TranscodingException {
        //initial capacity is very roughly and arbitrarily initialized (4 bytes on average per value)
        final ByteBufOutputStream out = new ByteBufOutputStream(Unpooled.buffer(4 * multiValue.size()));
        //Note this OutputStream implementation doesn't implement flush() nor close(), so they are left out.
        try {
            for (Iterator<?> iterator = multiValue.iterator(); iterator.hasNext(); ) {
                Object o = iterator.next();
                writeValueIntoStream(out, o);
                if (iterator.hasNext()) {
                   out.writeBytes(",");
                }
            }
            return out.buffer();
        } catch (Exception e) {
            throw new TranscodingException(transcodingErrorMessage, e);
        }
        //changing the OutputStream concrete implementation would probably require to close() in a finally block
    }

    /**
     * Deserializes a byte array into a generic Object. The provided offset and length must be considered when
     * processing the array, which may hold more data that just the value to deserialize.
     *
     * Note that he byte array should not be considered reliable for long-term usage (eg. backing a String) as
     * it might be tied to the original {@link ByteBuf}, which will get released from the heap.
     *
     * If the array represents a non-scalar value, implementations may choose different classes like a
     * {@link JsonObject} or a {@link Map} to instantiate it.
     *
     * This method is called by {@link #decodeWithMessage(ByteBuf, Class, String)} when the clazz parameter is
     * <code>Object.class</code>.
     *
     * @param byteArray the array of bytes containing the value to deserialize (you'll need to copy it if long term
     *                  usage is needed).
     * @param offset the offset in the array at which the value starts.
     * @param length the number of bytes after the offset that represents the value.
     * @return an instance of a suitable generic Object representation of the value.
     */
    protected abstract Object byteArrayToGenericObject(byte[] byteArray, int offset, int length) throws IOException;

    /**
     * Deserializes a byte array into a specific class instance. The provided offset and length must be considered
     * when processing the array, which may hold more data that just the value to deserialize.
     *
     * Note that he byte array should not be considered reliable for long-term usage (eg. backing a String) as
     * it might be tied to the original {@link ByteBuf}, which will get released from the heap.
     *
     * This method is called by {@link #decodeWithMessage(ByteBuf, Class, String)} when the clazz parameter isn't
     * <code>Object.class</code>.
     *
     * @param byteArray the array of bytes containing the value to deserialize (you'll need to copy it if long term
     *                  usage is needed).
     * @param offset the offset in the array at which the value starts.
     * @param length the number of bytes after the offset that represents the value.
     * @param clazz the {@link Class} to deserialize to.
     * @return an instance of a suitable generic Object representation of the value.
     */
    protected abstract <T> T byteArrayToClass(byte[] byteArray, int offset, int length, Class<? extends T> clazz) throws IOException;

    /**
     * Serializes a single value object as an array of bytes. The array will be backing a {@link ByteBuf}, so
     * modifications to the array will be visible in the ByteBuf.
     *
     * @param value the value object to serialize.
     * @return the array of bytes representing the serialized value object.
     */
    protected abstract <T> byte[] writeValueAsBytes(T value) throws IOException;

    /**
     * Serializes a single object out of a sequence of multiple values, into the sequence's {@link OutputStream}.
     * Implementation should simply write the bytes corresponding to the serialized value object into the stream.
     *
     * @param out the {@link OutputStream} of bytes representing a JSON sequence of serialized values.
     * @param o the value among the sequence that is currently serialized.
     */
    protected abstract void writeValueIntoStream(OutputStream out, Object o) throws IOException;
}
