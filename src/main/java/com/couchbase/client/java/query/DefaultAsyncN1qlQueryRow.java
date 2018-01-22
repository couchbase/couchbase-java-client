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
package com.couchbase.client.java.query;

import java.io.IOException;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.transcoder.JacksonTransformers;

@InterfaceStability.Committed
@InterfaceAudience.Public
public class DefaultAsyncN1qlQueryRow implements AsyncN1qlQueryRow {

    private static final ObjectMapper OBJECT_MAPPER = JacksonTransformers.MAPPER;

    private JsonObject value = null;
    private final byte[] byteValue;

    public DefaultAsyncN1qlQueryRow(byte[] value) {
        this.byteValue = value;
    }

    @Override
    public byte[] byteValue() {
        return byteValue;
    }

    /**
     * Return the {@link JsonObject} representation of the JSON corresponding to this row.
     * The {@link JsonObject} is lazily created from {@link #byteValue()} the first time it is requested.
     *
     * @return the JsonObject representation of the value.
     * @throws TranscodingException if the lazy deserialization couldn't be performed due to a Jackson error.
     */
    @Override
    public JsonObject value() {
        if (byteValue == null) {
            return null;
        } else if (value == null) {
            try {
                value = OBJECT_MAPPER.readValue(byteValue, JsonObject.class);
                return value;
            } catch (IOException e) {
                throw new TranscodingException("Error deserializing row value from bytes to JsonObject \""
                  + new String(byteValue, CharsetUtil.UTF_8) + "\"", e);
            }
        } else {
            return value;
        }
    }

    @Override
    public String toString() {
        return byteValue == null ? "null" : new String(byteValue, CharsetUtil.UTF_8);
    }
}
