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
package com.couchbase.client.java;

import com.couchbase.client.java.error.FlushDisabledException;
import com.couchbase.client.java.util.CouchbaseTestContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assume.assumeFalse;

/**
 * Verifies the exception surface of running flush on a flush-disabled bucket.
 *
 * @author Michael Nitschinger
 * @since 2.3.0
 */
public class FlushDisabledTest {

    private static CouchbaseTestContext ctx;

    @BeforeClass
    public static void init() {
        assumeFalse(CouchbaseTestContext.isMockEnabled());

        ctx = CouchbaseTestContext.builder()
            .bucketName("FlushDisabled")
            .enableFlush(false)
            .adhoc(true)
            .flushOnInit(false)
            .build();
    }

    @AfterClass
    public static void cleanup() {
        if (ctx != null) {
            ctx.destroyBucketAndDisconnect();
        }
    }

    @Test(expected = FlushDisabledException.class)
    public void shouldFailOnDisabledFlush() {
        ctx.bucketManager().flush();
    }
}
