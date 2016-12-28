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
package com.couchbase.client.java;

import com.couchbase.client.core.CouchbaseException;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements a {@link ConnectionString}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
@Deprecated
public class ConnectionString {

    public static final String DEFAULT_SCHEME = "couchbase://";

    private final Scheme scheme;
    private final List<InetSocketAddress> hosts;
    private final Map<String, String> params;

    protected ConnectionString(final String input) {
        this.scheme = parseScheme(input);
        this.hosts = parseHosts(input);
        this.params = parseParams(input);
    }

    public static ConnectionString create(final String input) {
        return new ConnectionString(input);
    }

    public static ConnectionString fromHostnames(final List<String> hostnames) {
        StringBuilder sb = new StringBuilder(DEFAULT_SCHEME);
        for (int i = 0; i < hostnames.size(); i++) {
            sb.append(hostnames.get(i));
            if (i < hostnames.size()-1) {
                sb.append(",");
            }
        }
        return create(sb.toString());
    }

    static Scheme parseScheme(final String input) {
        if (input.startsWith("couchbase://")) {
            return Scheme.COUCHBASE;
        } else if (input.startsWith("couchbases://")) {
            return Scheme.COUCHBASES;
        } else if (input.startsWith("http://")) {
            return Scheme.HTTP;
        } else {
            throw new CouchbaseException("Could not parse Scheme of connection string: " + input);
        }
    }

    static List<InetSocketAddress> parseHosts(final String input) {
        String schemeRemoved = input.replaceAll("\\w+://", "");
        String paramsRemoved = schemeRemoved.replaceAll("\\?.*", "");
        String[] splitted = paramsRemoved.split(",");

        List<InetSocketAddress> hosts = new ArrayList<InetSocketAddress>();
        for (int i = 0; i < splitted.length; i++) {
            if (splitted[i] == null || splitted[i].isEmpty()) {
                continue;
            }
            String[] parts = splitted[i].split(":");
            if (parts.length == 1) {
                hosts.add(InetSocketAddress.createUnresolved(parts[0], 0));
            } else {
                hosts.add(InetSocketAddress.createUnresolved(parts[0], Integer.parseInt(parts[1])));
            }
        }
        return hosts;
    }

    static Map<String, String> parseParams(final String input) {
        try {
            String[] parts = input.split("\\?");
            Map<String, String> params = new HashMap<String, String>();
            if (parts.length > 1) {
                String found = parts[1];
                String[] exploded = found.split("&");
                for (int i = 0; i < exploded.length; i++) {
                    String[] pair = exploded[i].split("=");
                    params.put(pair[0], pair[1]);
                }
            }
            return params;
        } catch(Exception ex) {
            throw new CouchbaseException("Could not parse Params of connection string: " + input, ex);
        }
    }

    public Scheme scheme() {
        return scheme;
    }

    public List<InetSocketAddress> hosts() {
        return hosts;
    }

    public Map<String, String> params() {
        return params;
    }

    public enum Scheme {
        HTTP,
        COUCHBASE,
        COUCHBASES
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ConnectionString{");
        sb.append("scheme=").append(scheme);
        sb.append(", hosts=").append(hosts);
        sb.append(", params=").append(params);
        sb.append('}');
        return sb.toString();
    }
}
