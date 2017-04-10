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
package com.couchbase.client.java.query.dsl.element;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.deps.io.netty.util.internal.StringUtil;
import com.couchbase.client.java.query.Statement;

@InterfaceStability.Experimental
@InterfaceAudience.Private
public class IntersectElement implements Element {
    private final boolean all;
    private final String with;

    public IntersectElement(final boolean all) {
        this.all = all;
        this.with = null;
    }

    public IntersectElement(final boolean all, final String with) {
        this.all = all;
        this.with = with;
    }

    public IntersectElement(final boolean all, final Statement with) {
        this.all = all;
        this.with = with.toString();
    }

    @Override
    public String export() {
        final StringBuilder sb = new StringBuilder();

        sb.append("INTERSECT");

        if (all) {
            sb.append(" ALL");
        }

        if (!StringUtil.isNullOrEmpty(with)) {
            sb.append(" ");
            sb.append(with);
        }

        return sb.toString();
    }
}
