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

import com.couchbase.client.java.CouchbaseAsyncBucket;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.FromElement;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DefaultFromPath extends DefaultLetPath implements FromPath {

    public DefaultFromPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public AsPath from(String from) {
        element(new FromElement(from));
        return new DefaultAsPath(this);
    }

    @Override
    public AsPath from(Expression from) {
        element(new FromElement(from.toString()));
        return new DefaultAsPath(this);
    }

    @Override
    public AsPath fromCurrentBucket() {
        element(new FromElement(CouchbaseAsyncBucket.CURRENT_BUCKET_IDENTIFIER));
        return new DefaultAsPath(this);
    }
}
