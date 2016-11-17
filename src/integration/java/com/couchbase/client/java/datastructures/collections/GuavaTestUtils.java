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
package com.couchbase.client.java.datastructures.collections;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.google.common.collect.testing.SampleElements;

/**
 * An abstract base for guava tests that has a uuid field (to be filled by implementations)
 * and exposes a small representative test sample.
 */
public abstract class GuavaTestUtils {

    private GuavaTestUtils() { }

    protected static SampleElements<Object> samples = new SampleElements<Object>(
            123,
            "aString",
            true,
            JsonObject.create().put("sub", "value"),
            JsonArray.from("A" ,"B", "C")
    );

    protected static SampleElements<Object> samplesWithoutJsonValues = new SampleElements<Object>(
            123,
            "aString",
            true,
            4.56,
            "foo"
    );
}
