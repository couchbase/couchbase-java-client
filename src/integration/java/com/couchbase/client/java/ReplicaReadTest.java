/**
 * Copyright (C) 2015 Couchbase, Inc.
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
package com.couchbase.client.java;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.util.ClusterDependentTest;
import org.junit.Before;
import org.junit.Test;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

/**
 * Verifies the functionality of the replica read functionality.
 *
 * @author Michael Nitschinger
 * @since 2.2.0
 */
public class ReplicaReadTest extends ClusterDependentTest {

    private ReplicateTo replicateTo;
    private int numReplicas;
    private boolean nodesMatchReplicas;

    @Before
    public void setup() {
        numReplicas = bucket().bucketManager().info().replicaCount();

        switch (numReplicas) {
            case 0:
                replicateTo = ReplicateTo.NONE;
                break;
            case 1:
                replicateTo = ReplicateTo.ONE;
                break;
            case 2:
                replicateTo = ReplicateTo.TWO;
                break;
            case 3:
                replicateTo = ReplicateTo.THREE;
        }

        int numNodes = bucket().bucketManager().info().nodeCount();
        nodesMatchReplicas = numNodes >= (numReplicas + 1);
    }

    @Test
    public void shouldReadAllAsListFromReplica() {
        assumeTrue("Number of nodes does not match the replica setup", nodesMatchReplicas);

        String id = "replica-doc1";
        JsonDocument stored = bucket().upsert(
            JsonDocument.create(id, JsonObject.create().put("foo", "bar")),
            replicateTo
        );
        assertEquals(id, stored.id());

        List<JsonDocument> fromReplica = bucket().getFromReplica(id, ReplicaMode.ALL);
        assertEquals(numReplicas + 1, fromReplica.size());
        for (JsonDocument found : fromReplica) {
            assertEquals(id, found.id());
            assertEquals(stored.cas(), found.cas());
            assertEquals(stored.content(), found.content());
        }
    }

    @Test
    public void shouldReadAllAsIteratorFromReplica() {
        assumeTrue("Number of nodes does not match the replica setup", nodesMatchReplicas);

        String id = "replica-doc2";
        JsonDocument stored = bucket().upsert(
            JsonDocument.create(id, JsonObject.create().put("foo", "bar")),
            replicateTo
        );
        assertEquals(id, stored.id());

        Iterator<JsonDocument> fromReplica = bucket().getFromReplica(id);

        int size = 0;
        while(fromReplica.hasNext()) {
            size++;
            JsonDocument found = fromReplica.next();
            assertEquals(id, found.id());
            assertEquals(stored.cas(), found.cas());
            assertEquals(stored.content(), found.content());
        }
        assertEquals(numReplicas + 1, size);
    }


}
