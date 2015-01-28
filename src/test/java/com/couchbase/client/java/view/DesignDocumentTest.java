/**
 * Copyright (C) 2014 Couchbase, Inc.
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
