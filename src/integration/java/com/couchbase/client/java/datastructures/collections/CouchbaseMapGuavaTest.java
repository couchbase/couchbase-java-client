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


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.util.CouchbaseTestContext;
import com.couchbase.client.java.util.features.CouchbaseFeature;
import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.TestStringMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import junit.framework.TestSuite;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * Tests the functionality of {@link CouchbaseMap} using guava-testlib's testsuite
 * generator for maps.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ CouchbaseMapGuavaTest.GuavaTests.class })
@Ignore
public class CouchbaseMapGuavaTest {

    //the holder for the guava-generated test suite
    public static class GuavaTests {


        private static int testCount;
        private static String uuid;

        @Test
        @Ignore
        //fixes "All Unit Tests" runs in IntelliJ complaining about no test method found
        public void noop() { }

        public static TestSuite suite() {
            final CouchbaseTestContext ctx = CouchbaseTestContext.builder()
                .bucketQuota(100)
                .bucketReplicas(1)
                .bucketType(BucketType.COUCHBASE)
                .build();

            ctx.ignoreIfMissing(CouchbaseFeature.SUBDOC);


            TestSuite suite = new MapTestSuiteBuilder<String, String>()
                    .using(new TestStringMapGenerator() {
                        @Override
                        protected Map<String, String> create(Map.Entry<String, String>[] entries) {
                            HashMap<String, String> tempMap = new HashMap<String, String>(entries.length);
                            for (Map.Entry<String, String> entry : entries) {
                                tempMap.put(entry.getKey(), entry.getValue());
                            }
                            Map<String, String> map = new CouchbaseMap<String>(uuid, ctx.bucket(), tempMap);
                            return map;
                        }
                    })
                    .withSetUp(new Runnable() {
                        @Override
                        public void run() {
                            uuid = UUID.randomUUID().toString();
                        }
                    })
                    .withTearDown(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ctx.bucket().remove(uuid);
                            } catch (DocumentDoesNotExistException e) {
                                //ignore
                            }
                            testCount--;
                            if (testCount < 1) {
                                ctx.destroyBucketAndDisconnect();
                            }
                        }
                    })
                    .named("CouchbaseMap")
                    .withFeatures(
                            MapFeature.GENERAL_PURPOSE,
                            MapFeature.ALLOWS_NULL_VALUES,
                            MapFeature.RESTRICTS_KEYS,
                            MapFeature.RESTRICTS_VALUES,
                            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                            CollectionSize.ANY)
                    .createTestSuite();

            testCount = suite.countTestCases() - suite.testCount();
            return suite;
        }
    }
}