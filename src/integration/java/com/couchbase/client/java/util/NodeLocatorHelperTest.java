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

import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the functionality of the {@link NodeLocatorHelper}.
 *
 * @author Michael Nitschinger
 * @since 2.1.0
 */
public class NodeLocatorHelperTest extends ClusterDependentTest {

    private NodeLocatorHelper helper;

    @Before
    public void setup() {
        helper = NodeLocatorHelper.create(bucket());
    }

    @Test
    public void shouldListAllNodes() {
        List<InetAddress> expected = bucketManager().info().nodeList();

        assertFalse(helper.nodes().isEmpty());
        assertEquals(expected, helper.nodes());
    }

    @Test
    public void shouldLocateActive() {
        InetAddress node = helper.activeNodeForId("foobar");
        assertTrue(helper.nodes().contains(node));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAcceptHigherReplicaNum() {
        helper.replicaNodeForId("foo", 4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAcceptLowerReplicaNum() {
        helper.replicaNodeForId("foo", 0);
    }

}