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
package com.couchbase.client.java.util.features;

import static org.junit.Assert.*;

import com.couchbase.client.java.cluster.DefaultClusterInfo;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import org.junit.Test;

/**
 * Verifies the behavior of {@link CouchbaseFeature}-detection related methods (eg. in {@link DefaultClusterInfo}.
 *
 * @author Simon Baslé
 * @since 2.1.0
 */
public class FeaturesTest {

    @Test
    public void shouldDetectCorrectMinimumDespiteGarbage() throws Exception {
        JsonObject node1 = JsonObject.create()
                .put("version", "3.0.2");
        JsonObject node2 = JsonObject.create()
                .put("version", "1.4.6-dp_1324");
        DefaultClusterInfo info = new DefaultClusterInfo(
                JsonObject.create().put("nodes", JsonArray.from(node1, node2)));

        assertEquals(new Version(1, 4, 6), info.getMinVersion());
        assertFalse(info.checkAvailable(CouchbaseFeature.SPATIAL_VIEW));
    }

    @Test
    public void shouldReturnFalseIfBadInfo() {
        JsonObject goodNode = JsonObject.create()
                                     .put("version", "3.0.2");
        JsonObject badNode = JsonObject.create()
                                     .put("nope", "1.4.6-dp_1324");
        DefaultClusterInfo info1 = new DefaultClusterInfo(
                JsonObject.create().put("nodess", JsonArray.from(goodNode)));
        DefaultClusterInfo info2 = new DefaultClusterInfo((JsonObject.create()
            .put("nodes", "string")));
        DefaultClusterInfo info3 = new DefaultClusterInfo((JsonObject.create()
            .put("nodes", JsonArray.from("notANode"))));
        DefaultClusterInfo info4 = new DefaultClusterInfo(JsonObject.create()
            .put("nodes", JsonArray.from(badNode)));

        assertEquals(Version.NO_VERSION, info1.getMinVersion());
        assertEquals(Version.NO_VERSION, info2.getMinVersion());
        assertEquals(Version.NO_VERSION, info3.getMinVersion());
        assertEquals(Version.NO_VERSION, info4.getMinVersion());

        assertFalse(info1.checkAvailable(CouchbaseFeature.KV));
        assertFalse(info2.checkAvailable(CouchbaseFeature.KV));
        assertFalse(info3.checkAvailable(CouchbaseFeature.KV));
        assertFalse(info4.checkAvailable(CouchbaseFeature.KV));

    }

    @Test
    public void shouldReturnFalseIfBadVersionFormat() {
        JsonObject node1 = JsonObject.create().put("version", "3.0.2");
        JsonObject node2 = JsonObject.create().put("version", "z.4.6-dp_1324"); //bad
        DefaultClusterInfo info1 = new DefaultClusterInfo(
                JsonObject.create().put("nodes", JsonArray.from(node1, node2)));
        JsonObject node3 = JsonObject.create().put("version", "noversion");
        DefaultClusterInfo info2 = new DefaultClusterInfo(
                JsonObject.create().put("nodes", JsonArray.from(node3)));

        assertEquals(Version.NO_VERSION, info1.getMinVersion());
        assertFalse(info1.checkAvailable(CouchbaseFeature.KV));

        assertEquals(Version.NO_VERSION, info2.getMinVersion());
        assertFalse(info2.checkAvailable(CouchbaseFeature.KV));
    }
}