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
 * An utility class to execute generic HTTP calls on a cluster's REST API.
 *
 * @author Simon Baslé
 * @since 2.3.2
 */
@InterfaceAudience.Public
@InterfaceStability.Experimental
public abstract class AbstractClusterApiClient<T extends RestBuilderMarker> {

    protected final String username;
    protected final String password;
    protected final ClusterFacade core;

    /**
     * Build a new {@link AbstractClusterApiClient} to work with a given {@link ClusterFacade}.
     *
     * @param username the login to use for REST api calls (eg. administrative username).
     * @param password the password associated with the username.
     * @param core the {@link ClusterFacade} through which to sen requests.
     */
    protected AbstractClusterApiClient(String username, String password, ClusterFacade core) {
        this.username = username;
        this.password = password;
        this.core = core;
    }

    /**
     * Prepare a GET request for the cluster API on a given path.
     *
     * The elements of the path are processed as follows:
     *  - if an element starts with a slash, it is kept. Otherwise a trailing slash is added.
     *  - if an element ends with a slash, it is removed.
     *  - if an element is null, it is ignored.
     *
     * @param paths the elements of the path.
     * @return a {@link RestBuilderMarker} used to further configure the request (either sync or async). Use its
     *   <code>execute()</code> methods to trigger the request.
     */
    public T get(String... paths) {
        return createBuilder(HttpMethod.GET, buildPath(paths));
    }

    /**
     * Prepare a POST request for the cluster API on a given path.
     *
     * The elements of the path are processed as follows:
     *  - if an element starts with a slash, it is kept. Otherwise a trailing slash is added.
     *  - if an element ends with a slash, it is removed.
     *  - if an element is null, it is ignored.
     *
     * @param paths the elements of the path.
     * @return a {@link RestBuilderMarker} used to further configure the request (either sync or async). Use its
     *   <code>execute()</code> methods to trigger the request.
     */
    public T post(String... paths) {
        return createBuilder(HttpMethod.POST, buildPath(paths));
    }

    /**
     * Prepare a PUT request for the cluster API on a given path.
     *
     * The elements of the path are processed as follows:
     *  - if an element starts with a slash, it is kept. Otherwise a trailing slash is added.
     *  - if an element ends with a slash, it is removed.
     *  - if an element is null, it is ignored.
     *
     * @param paths the elements of the path.
     * @return a {@link RestBuilderMarker} used to further configure the request (either sync or async). Use its
     *   <code>execute()</code> methods to trigger the request.
     */
    public T put(String... paths) {
        return createBuilder(HttpMethod.PUT, buildPath(paths));
    }

    /**
     * Prepare a DELETE request for the cluster API on a given path.
     *
     * The elements of the path are processed as follows:
     *  - if an element starts with a slash, it is kept. Otherwise a trailing slash is added.
     *  - if an element ends with a slash, it is removed.
     *  - if an element is null, it is ignored.
     *
     * @param paths the elements of the path.
     * @return a {@link RestBuilderMarker} used to further configure the request (either sync or async). Use its
     *   <code>execute()</code> methods to trigger the request.
     */
    public T delete(String... paths) {
        return createBuilder(HttpMethod.DELETE, buildPath(paths));
    }

    /**
     * Create the concrete {@link RestBuilderMarker builders} returned by concrete implementations.
     * Builders will be either capable of synchronous or asynchronous execution, depending on
     * type T.
     */
    protected abstract T createBuilder(HttpMethod method, String fullPath);

    /**
     * Assemble path elements to form an HTTP path:
     *  - if an element starts with a slash, it is kept. Otherwise a trailing slash is added.
     *  - if an element ends with a slash, it is removed.
     *  - if an element is null, it is ignored.
     *
     * @param paths the elements of the path.
     * @return returns the full path.
     */
    public static String buildPath(String... paths) {
        if (paths == null || paths.length == 0) {
            throw new IllegalArgumentException("Path must at least contain one element");
        }

        StringBuilder path = new StringBuilder();
        for (int i = 0; i < paths.length; i++) {
            String p = paths[i];
            if (p == null) continue;

            //skip separator if one already present
            if (p.charAt(0) != '/') {
                path.append('/');
            }
            //remove trailing /
            if (p.charAt(p.length() - 1) == '/') {
                path.append(p, 0, p.length() - 1);
            } else {
                path.append(p);
            }
        }
        return path.toString();
    }

}
