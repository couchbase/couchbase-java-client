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
package com.couchbase.client.java.util;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.repository.Repository;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Base test class for tests that need a working cluster reference.
 *
 * @deprecated prefer explicit @BeforeClass and @AfterClass using a {@link CouchbaseTestContext.Builder}.
 *
 * @author Michael Nitschinger
 * @author Simon Baslé
 */
@Deprecated
public class ClusterDependentTest {

    protected static CouchbaseTestContext ctx;

    @BeforeClass
    public static void connect() throws Exception {
        ctx = CouchbaseTestContext.builder()
                .bucketQuota(256)
                .bucketType(BucketType.COUCHBASE)
                .flushOnInit(true)
                .enableFlush(true)
                .build();
    }

    @AfterClass
    public static void disconnect() throws InterruptedException {
        ctx.disconnect();
    }

    public static String password() {
        return ctx.bucketPassword();
    }

    public static Cluster cluster() {
        return ctx.cluster();
    }

    public static Bucket bucket() {
        return ctx.bucket();
    }

    public static String bucketName() {
        return ctx.bucketName();
    }

    public static Repository repository() {
        return ctx.repository();
    }

    public static BucketManager bucketManager() {
        return ctx.bucketManager();
    }
}