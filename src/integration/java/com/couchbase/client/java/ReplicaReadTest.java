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
