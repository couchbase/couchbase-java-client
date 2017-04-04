/*
 * Copyright (c) 2017 Couchbase, Inc.
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
package com.couchbase.client.java.search.sort;

/**
 * Sort by the document ID.
 *
 * @author Michael Nitschinger
 * @since 2.4.5
 */
public class SearchSortId extends SearchSort {

    @Override
    protected String identifier() {
        return "id";
    }

    @Override
    public SearchSortId descending(boolean descending) {
        super.descending(descending);
        return this;
    }

}
