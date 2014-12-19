/*
 * Copyright (c) 2014 Couchbase, Inc.
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Representation of a software version, up to major.minor.patch granularity.
 *
 * @author Simon Baslé
 * @since 2.1.0
 */
public class Version implements Comparable<Version> {

    public static final Version NO_VERSION = new Version(0, 0, 0);

    private final int major;
    private final int minor;
    private final int patch;

    public Version(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    public int patch() {
        return patch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Version version = (Version) o;

        if (major != version.major) {
            return false;
        }
        if (minor != version.minor) {
            return false;
        }
        if (patch != version.patch) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + patch;
        return result;
    }

    @Override
    public int compareTo(Version o) {
        if (o == null) {
            return 1;
        }
        int ma = this.major - o.major;
        int mi = this.minor - o.minor;
        int pa = this.patch - o.patch;

        if (ma != 0) {
            return ma;
        } else if (mi != 0) {
            return mi;
        } else {
            return pa;
        }
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?.*");

    /**
     * Parses a {@link String} into a {@link Version}. This expects a version string in the form of
     * "X[.Y[.Z]][anything]". That is a major number, followed optionally by a minor only or a minor + a patch revision,
     * everything after that being completely ignored.
     *
     * For example, the following version strings are valid:
     *  - "3.a.2", but note it is considered only a major version since there's a char where minor number should be
     * (parsed into 3.0.0)
     *  - "2" (2.0.0)
     *  - "3.11" (3.11)
     *  - "1.2.3-SNAPSHOT-12.10.2014" (1.2.3)
     *
     * Bad version strings cause an {@link IllegalArgumentException}, whereas a null one will cause
     * a {@link NullPointerException}.
     *
     * @param versionString the string to parse into a Version.
     * @return the major.minor.patch Version corresponding to the string.
     * @throws IllegalArgumentException if the string cannot be correctly parsed into a Version
     * @throws NullPointerException if the string is null.
     */
    public static final Version parseVersion(String versionString) {
        if (versionString == null) {
            throw new NullPointerException();
        }

        Matcher matcher = VERSION_PATTERN.matcher(versionString);
        if (matcher.matches() && matcher.groupCount() == 3) {
            int major = Integer.parseInt(matcher.group(1));
            int minor = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
            int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            return new Version(major, minor, patch);
        } else {
            throw new IllegalArgumentException(
                    "Expected a version string starting with X[.Y[.Z]], was " + versionString);
        }
    }

}
