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
@InterfaceStability.Uncommitted
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
