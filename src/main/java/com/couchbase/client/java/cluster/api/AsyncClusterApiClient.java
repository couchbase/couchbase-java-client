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
package com.couchbase.client.java.cluster.api;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.deps.io.netty.handler.codec.http.HttpMethod;

/**
 * An utility class to execute generic HTTP calls asynchronously on a cluster's REST API.
 *
 * @author Simon Baslé
 * @since 2.3.2
 */
@InterfaceAudience.Public
@InterfaceStability.Experimental
public class AsyncClusterApiClient extends AbstractClusterApiClient<AsyncRestBuilder> {

    /**
     * Build a new {@link AsyncClusterApiClient} to work with a given {@link ClusterFacade}.
     *
     * @param username the login to use for REST api calls (eg. administrative username).
     * @param password the password associated with the username.
     * @param core     the {@link ClusterFacade} through which to send requests.
     */
    public AsyncClusterApiClient(String username, String password, ClusterFacade core) {
        super(username, password, core);
    }

    @Override
    protected AsyncRestBuilder createBuilder(HttpMethod method, String fullPath) {
        return new AsyncRestBuilder(this.username, this.password, this.core, method, fullPath);
    }
}
