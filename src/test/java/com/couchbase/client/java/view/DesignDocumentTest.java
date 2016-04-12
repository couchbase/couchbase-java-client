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
package com.couchbase.client.java.view;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the functionality of a {@link DesignDocument}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class DesignDocumentTest {

    @Test
    public void shouldEqualBasedOnItsProperties() {
        DesignDocument designDocument1 = DesignDocument.create("ddoc", Arrays.asList(DefaultView.create("foo", "bar")));
        DesignDocument designDocument2 = DesignDocument.create("ddoc", Arrays.asList(DefaultView.create("foo", "bar")));
        assertTrue(designDocument1.equals(designDocument2));

        designDocument1 = DesignDocument.create("foobar", Arrays.asList(DefaultView.create("foo", "bar")));
        designDocument2 = DesignDocument.create("ddoc", Arrays.asList(DefaultView.create("foo", "bar")));
        assertFalse(designDocument1.equals(designDocument2));

        designDocument1 = DesignDocument.create("ddoc", Arrays.asList(DefaultView.create("abc", "bar")));
        designDocument2 = DesignDocument.create("ddoc", Arrays.asList(DefaultView.create("foo", "bar")));
        assertFalse(designDocument1.equals(designDocument2));
    }

}
