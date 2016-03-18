/*
 * Copyright (C) 2016 Couchbase, Inc.
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

package com.couchbase.client.java.subdoc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;

/**
 * An internal representation of multiple values to insert in an array via the sub-document API.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Private
public class MultiValue<T> implements Iterable<T> {

    private final List<T> values;

    /**
     * Create MultiValue out of an enumeration of objects (as a vararg).
     */
    public MultiValue(T... values) {
        if (values == null || values.length < 1) {
            throw new IllegalArgumentException("MultiValue does not make sense with a null or empty array of elements");
        }
        this.values = new ArrayList<T>(values.length);
        Collections.addAll(this.values, values);
    }

    /**
     * Create MultiValue out of a Collection of objects.
     */
    public MultiValue(Collection<T> values) {
        if (values == null || values.size() < 1) {
            throw new IllegalArgumentException("MultiValue does not make sense with a null or empty collection of elements");
        }
        this.values = new ArrayList<T>(values);
    }

    /**
     * @return the size of this {@link MultiValue}, the number of iterable elements in it.
     */
    public int size() {
        return values.size();
    }

    @Override
    public Iterator<T> iterator() {
        return values.iterator();
    }
}
