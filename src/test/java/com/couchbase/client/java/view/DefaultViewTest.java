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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the functionality of a {@link DefaultView}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class DefaultViewTest {

    @Test
    public void shouldEqualBasedOnItsProperties() {
        View view1 = DefaultView.create("name", "map", "reduce");
        View view2 = DefaultView.create("name", "map", "reduce");
        assertTrue(view1.equals(view2));

        view1 = DefaultView.create("name", "map", "reduce");
        view2 = DefaultView.create("foobar", "map", "reduce");
        assertFalse(view1.equals(view2));

        view1 = DefaultView.create("name", "map", "reduce");
        view2 = DefaultView.create("name", "map");
        assertFalse(view1.equals(view2));

        view1 = DefaultView.create("name", "map", "reduce");
        view2 = DefaultView.create("name", "map", "foobar");
        assertFalse(view1.equals(view2));

        view1 = DefaultView.create("name", "map", "reduce");
        view2 = DefaultView.create("name", "foobar", "reduce");
        assertFalse(view1.equals(view2));
    }

}
