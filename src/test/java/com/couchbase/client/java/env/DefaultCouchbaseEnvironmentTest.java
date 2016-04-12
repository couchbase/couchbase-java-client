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
package com.couchbase.client.java.env;

import com.couchbase.client.core.env.CoreEnvironment;
import com.couchbase.client.core.env.DefaultCoreEnvironment;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the functionality of a {@link DefaultCouchbaseEnvironment}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class DefaultCouchbaseEnvironmentTest {

    @Test
    public void shouldApplyDefaultSettings() {
        CouchbaseEnvironment env = DefaultCouchbaseEnvironment.create();

        assertEquals(DefaultCouchbaseEnvironment.KEYVALUE_ENDPOINTS, env.kvEndpoints());
        assertNotNull(env.ioPool());
    }

    @Test
    public void shouldOverrideSettings() {
        CouchbaseEnvironment env = DefaultCouchbaseEnvironment.
            builder()
            .kvEndpoints(5)
            .build();

        assertEquals(5, env.kvEndpoints());
        assertNotNull(env.ioPool());
    }

    @Test
    public void systemPropertiesShouldTakePrecedence() {
        System.setProperty("com.couchbase.kvEndpoints", "10");
        CouchbaseEnvironment env = DefaultCouchbaseEnvironment.
            builder()
            .kvEndpoints(5)
            .build();

        assertEquals(10, env.kvEndpoints());
        System.clearProperty("com.couchbase.kvEndpoints");
    }

    @Test
    public void toStringShouldContainJavaClientSpecificParameters() {
        CouchbaseEnvironment env = DefaultCouchbaseEnvironment.create();

        assertTrue(env.toString().contains("kvTimeout="));
    }

    @Test
    public void testVersionInformationDifferentFromCore() {
        CoreEnvironment coreEnv = DefaultCoreEnvironment.create();
        CouchbaseEnvironment env = DefaultCouchbaseEnvironment.create();

        assertNotEquals(coreEnv.packageNameAndVersion(), env.packageNameAndVersion());
        assertNotEquals(coreEnv.userAgent(), env.userAgent());
        assertTrue(env.toString(), env.toString().contains(env.packageNameAndVersion()));
        assertEquals(coreEnv.coreVersion(), env.coreVersion());
        assertEquals(coreEnv.coreBuild(), env.coreBuild());
        assertNotEquals(coreEnv.coreVersion(), env.clientVersion());
        assertNotEquals(coreEnv.coreBuild(), env.clientBuild());
    }

    @Test
    public void testForcedVersionInformationSameWithCore() {
        CoreEnvironment coreEnv = DefaultCoreEnvironment.builder()
                .packageNameAndVersion("foo")
                .userAgent("bar")
                .build();

        CouchbaseEnvironment env = DefaultCouchbaseEnvironment.builder()
                .packageNameAndVersion("foo")
                .userAgent("bar")
                .build();

        assertEquals(coreEnv.packageNameAndVersion(), env.packageNameAndVersion());
        assertEquals(coreEnv.userAgent(), env.userAgent());
        assertTrue(env.toString(), env.toString().contains(coreEnv.packageNameAndVersion()));
        assertEquals(coreEnv.coreVersion(), env.coreVersion());
        assertEquals(coreEnv.coreBuild(), env.coreBuild());
        assertNotEquals(coreEnv.coreVersion(), env.clientVersion());
        assertNotEquals(coreEnv.coreBuild(), env.clientBuild());
    }
}