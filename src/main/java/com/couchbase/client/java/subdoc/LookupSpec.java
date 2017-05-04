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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.message.kv.subdoc.multi.Lookup;
import com.couchbase.client.core.message.kv.subdoc.multi.LookupCommand;
import com.couchbase.client.core.message.kv.subdoc.multi.LookupCommandBuilder;

/**
 * Internally represents a single lookup operation in a batch of subdocument operations.
 *
 * @author Michael Nitschinger
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Private
public class LookupSpec extends LookupCommand {

    public LookupSpec(Lookup type, String path) {
        super(new LookupCommandBuilder(type, path));
    }

    public LookupSpec(Lookup type, String path, SubdocOptionsBuilder builder) {
        super(new LookupCommandBuilder(type, path).xattr(builder.xattr()));
    }

    @Override
    public String toString() {
        return "{" + lookup() + ":" + path() + "}";
    }
}
