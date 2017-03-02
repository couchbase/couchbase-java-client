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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.TranscodingException;

@InterfaceStability.Committed
@InterfaceAudience.Public
public class DefaultN1qlQueryRow implements N1qlQueryRow {


    private final AsyncN1qlQueryRow asyncRow;

    public DefaultN1qlQueryRow(AsyncN1qlQueryRow asyncRow) {
        this.asyncRow = asyncRow;
    }

    @Override
    public byte[] byteValue() {
        return asyncRow.byteValue();
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
        return asyncRow.value();
    }

    @Override
    public String toString() {
        return asyncRow.toString();
    }
}
