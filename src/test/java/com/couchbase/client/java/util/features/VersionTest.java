/*
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
package com.couchbase.client.java.util.features;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Verifies the parsing of {@link Version}
 *
 * @author Simon Baslé
 * @since 2.1.0
 */
public class VersionTest {

    @Test
    public void shouldParseMajorMinorPatch() {
        Version version = Version.parseVersion("1.2.3");
        assertEquals("1.2.3", version.toString());
        assertEquals(1, version.major());
        assertEquals(2, version.minor());
        assertEquals(3, version.patch());
    }

    @Test
    public void shouldParseMajorMinor() {
        Version version = Version.parseVersion("1.2");
        assertEquals("1.2.0", version.toString());
        assertEquals(1, version.major());
        assertEquals(2, version.minor());
        assertEquals(0, version.patch());
    }

    @Test
    public void shouldParseMajor()  {
        Version version = Version.parseVersion("1");
        assertEquals("1.0.0", version.toString());
        assertEquals(1, version.major());
        assertEquals(0, version.minor());
        assertEquals(0, version.patch());
    }

    @Test
    public void shouldParseMajorMinorPatchGarbage() {
        Version version = Version.parseVersion("1.2.3z-5.4");
        assertEquals("1.2.3", version.toString());
        assertEquals(1, version.major());
        assertEquals(2, version.minor());
        assertEquals(3, version.patch());
    }

    @Test
    public void shouldParseMajorMinorGarbage() {
        Version version = Version.parseVersion("1.2z-5.4");
        assertEquals("1.2.0", version.toString());
        assertEquals(1, version.major());
        assertEquals(2, version.minor());
        assertEquals(0, version.patch());
    }

    @Test
    public void shouldParseMajorGarbage() {
        Version version = Version.parseVersion("1z-5.4");
        assertEquals("1.0.0", version.toString());
        assertEquals(1, version.major());
        assertEquals(0, version.minor());
        assertEquals(0, version.patch());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNotStartingWithMajor() {
        Version.parseVersion("a3.2.4");
    }

    @Test(expected = NullPointerException.class)
    public void shouldNullPointerOnNullVersionString() {
        Version.parseVersion(null);
    }

}