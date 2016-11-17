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

import java.util.List;
import java.util.UUID;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.TestStringListGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;
import junit.framework.TestSuite;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * Tests the functionality of {@link CouchbaseArrayList} using guava-testlib's testsuite
 * generator for lists.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ CouchbaseArrayListGuavaTest.GuavaTests.class })
public class CouchbaseArrayListGuavaTest {

    //the holder for the guava-generated test suite
    public static class GuavaTests {

        private static Cluster cluster = CouchbaseCluster.create();
        private static Bucket bucket = cluster.openBucket();
        private static int testCount;

        private static String uuid;

        @Test
        @Ignore
        //fixes "All Unit Tests" runs in IntelliJ complaining about no test method found
        public void noop() { }

        public static TestSuite suite() {
            TestSuite suite = new ListTestSuiteBuilder<String>()
                    .using(new TestStringListGenerator() {
                        @Override
                        protected List<String> create(String[] elements) {
                            CouchbaseArrayList<String> l = new CouchbaseArrayList<String>(uuid, bucket, elements);
                            return l;
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
                                bucket.remove(uuid);
                            } catch (DocumentDoesNotExistException e) {
                                //ignore
                            }
                            testCount--;
                            if (testCount < 1) {
                                cluster.disconnect();
                            }
                        }
                    })
                    .named("CouchbaseArrayList")
                    .withFeatures(
                            ListFeature.SUPPORTS_SET,
                            CollectionFeature.SUPPORTS_ADD,
                            CollectionFeature.SUPPORTS_REMOVE,
                            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                            ListFeature.SUPPORTS_ADD_WITH_INDEX,
                            ListFeature.SUPPORTS_REMOVE_WITH_INDEX,
                            CollectionFeature.RESTRICTS_ELEMENTS,
                            CollectionFeature.ALLOWS_NULL_VALUES,
                            CollectionSize.ANY)
                    .createTestSuite();

            testCount = suite.countTestCases() - suite.testCount();
            return suite;
        }
    }

}
