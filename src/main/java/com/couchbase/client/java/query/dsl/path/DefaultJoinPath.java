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

import com.couchbase.client.java.query.dsl.element.AsElement;
import com.couchbase.client.java.query.dsl.element.NestedLoopJoinHintElement;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DefaultJoinPath extends DefaultKeysPath implements JoinPath {

    public DefaultJoinPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public JoinPath as(String alias) {
        element(new AsElement(alias));
        return new DefaultJoinPath(this);
    }

    @Override
    public KeysPath useHash(HashSide side) {
        element(new HashJoinHintElement(side));
        return new DefaultKeysPath(this);
    }

    @Override
    public KeysPath useNestedLoop() {
        element(new NestedLoopJoinHintElement());
        return new DefaultKeysPath(this);
    }
}
