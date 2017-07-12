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
import java.util.Set;
import java.util.UUID;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.util.CouchbaseTestContext;
import com.couchbase.client.java.util.features.CouchbaseFeature;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.SetFeature;
import junit.framework.TestSuite;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * Tests the functionality of {@link CouchbaseArraySet} using guava-testlib's testsuite
 * generator for sets.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({CouchbaseArraySetGuavaTest.GuavaTests.class})
@Ignore
public class CouchbaseArraySetGuavaTest {

    //the holder for the guava-generated test suite
    public static class GuavaTests {

        private static int testCount;

        private static String uuid;

        @Test
        @Ignore
        //fixes "All Unit Tests" runs in IntelliJ complaining about no test method found
        public void noop() {
        }

        public static TestSuite suite() {
            final CouchbaseTestContext ctx = CouchbaseTestContext.builder()
                .bucketQuota(100)
                .bucketReplicas(1)
                .bucketType(BucketType.COUCHBASE)
                .build();

            ctx.ignoreIfMissing(CouchbaseFeature.SUBDOC);

            TestSuite suite = new SetTestSuiteBuilder<Object>()
                    .using(new TestSetGenerator<Object>() {
                        @Override
                        public Set<Object> create(Object... elements) {
                            CouchbaseArraySet<Object> set = new CouchbaseArraySet<Object>(uuid, ctx.bucket(), null);
                            for (Object o : elements) {
                                set.add(o);
                            }
                            return set;
                        }

                        @Override
                        public SampleElements<Object> samples() {
                            return GuavaTestUtils.samplesWithoutJsonValues;
                        }

                        @Override
                        public Object[] createArray(int length) {
                            return new Object[length];
                        }

                        @Override
                        public Iterable<Object> order(List<Object> insertionOrder) {
                            return insertionOrder;
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
                    .named("CouchbaseArraySet")
                    .withFeatures(
                            SetFeature.GENERAL_PURPOSE,
                            CollectionFeature.RESTRICTS_ELEMENTS,
                            CollectionFeature.ALLOWS_NULL_VALUES,
                            CollectionSize.ANY)
                    .createTestSuite();

            testCount = suite.countTestCases() - suite.testCount();
            return suite;
        }
    }
}