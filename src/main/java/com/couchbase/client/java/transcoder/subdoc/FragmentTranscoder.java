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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.subdoc.DocumentFragment;
import com.couchbase.client.java.subdoc.LookupInBuilder;
import com.couchbase.client.java.subdoc.MutateInBuilder;

/**
 * An interface for transcoding sub-document fragments (as read and written by the subdocument API, eg. in
 * {@link MutateInBuilder} or {@link LookupInBuilder}).
 *
 * This is used internally by the bucket to encode fragments (for mutations) or decode fragments returned by the server
 * in order to instantiate a {@link DocumentFragment}.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface FragmentTranscoder {

    /**
     * Decode content in a {@link ByteBuf} **without releasing it**. Suitable for populating a
     * {@link DocumentFragment}'s content.
     *
     * @param encoded the encoded fragment value (will not be released).
     * @param clazz the target class for decoded value. Using {@link Object Object.class} implies a generic decode,
     *              where dictionaries are represented by {@link JsonObject} and arrays by {@link JsonArray}.
     * @param <T> the type of the decoded fragment.
     * @return a decoded fragment.
     * @throws TranscodingException if the decoding couldn't happen.
     */
    <T> T decode(ByteBuf encoded, Class<? extends T> clazz) throws TranscodingException;

    /**
     * Decode content in a {@link ByteBuf} **without releasing it**. Suitable for populating a
     * {@link DocumentFragment}'s content.
     *
     * @param encoded the encoded fragment value (will not be released).
     * @param clazz the target class for decoded value. Using {@link Object Object.class} implies a generic decode,
     *              where dictionaries are represented by {@link JsonObject} and arrays by {@link JsonArray}.
     * @param transcodingErrorMessage the error message to be used in the thrown {@link TranscodingException} if the
     *                                decoding couldn't happen.
     * @param <T> the type of the decoded fragment.
     * @return a decoded fragment.
     * @throws TranscodingException if the decoding couldn't happen.
     */
    <T> T decodeWithMessage(ByteBuf encoded, Class<? extends T> clazz, String transcodingErrorMessage) throws TranscodingException;

    /**
     * Encode a value to a {@link ByteBuf} suitable for use in the sub-document protocol.
     *
     * @param value the value to encode.
     * @param <T> the type of the fragment being encoded.
     * @return a {@link ByteBuf} representation of the fragment value.
     * @throws TranscodingException if the encoding couldn't happen.
     */
    <T> ByteBuf encode(T value) throws TranscodingException;


    /**
     * Encode a value to a {@link ByteBuf} suitable for use in the sub-document protocol.
     *
     * @param value the value to encode.
     * @param transcodingErrorMessage the error message to be used in the thrown {@link TranscodingException} if the
     *                                encoding couldn't happen.
     * @param <T> the type of the fragment being encoded.
     * @return a {@link ByteBuf} representation of the fragment value.
     * @throws TranscodingException if the encoding couldn't happen.
     */
    <T> ByteBuf encodeWithMessage(T value, String transcodingErrorMessage) throws TranscodingException;
}
