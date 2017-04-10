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
package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.element.ExceptElement;
import com.couchbase.client.java.query.dsl.element.IntersectElement;
import com.couchbase.client.java.query.dsl.element.UnionElement;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DefaultSelectResultPath extends DefaultOrderByPath implements SelectResultPath {

    public DefaultSelectResultPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public SelectPath union() {
        element(new UnionElement(false));
        return new DefaultSelectPath(this);
    }

    @Override
    public SelectPath unionAll() {
        element(new UnionElement(true));
        return new DefaultSelectPath(this);
    }

    @Override
    public SelectPath intersect() {
        element(new IntersectElement(false));
        return new DefaultSelectPath(this);
    }

    @Override
    public SelectPath intersectAll() {
        element(new IntersectElement(true));
        return new DefaultSelectPath(this);
    }

    @Override
    public SelectPath except() {
        element(new ExceptElement(false));
        return new DefaultSelectPath(this);
    }

    @Override
    public SelectPath exceptAll() {
        element(new ExceptElement(true));
        return new DefaultSelectPath(this);
    }

    @Override
    public SelectResultPath union(final SelectResultPath path) {
        element(new UnionElement(false, path));
        return new DefaultSelectResultPath(this);
    }

    @Override
    public SelectResultPath unionAll(final SelectResultPath path) {
        element(new UnionElement(true, path));
        return new DefaultSelectResultPath(this);
    }

    @Override
    public SelectResultPath intersect(final SelectResultPath path) {
        element(new IntersectElement(false, path));
        return new DefaultSelectResultPath(this);
    }

    @Override
    public SelectResultPath intersectAll(final SelectResultPath path) {
        element(new IntersectElement(true, path));
        return new DefaultSelectResultPath(this);
    }

    @Override
    public SelectResultPath except(final SelectResultPath path) {
        element(new ExceptElement(false, path));
        return new DefaultSelectResultPath(this);
    }

    @Override
    public SelectResultPath exceptAll(final SelectResultPath path) {
        element(new ExceptElement(true, path));
        return new DefaultSelectResultPath(this);
    }
}
