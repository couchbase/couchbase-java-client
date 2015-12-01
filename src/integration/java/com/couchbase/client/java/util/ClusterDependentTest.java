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

    private static CouchbaseTestContext ctx;

    @BeforeClass
    public static void connect() throws Exception {
        ctx = CouchbaseTestContext.builder()
                .bucketQuota(256)
                .bucketType(BucketType.COUCHBASE)
                .flushOnInit(true)
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