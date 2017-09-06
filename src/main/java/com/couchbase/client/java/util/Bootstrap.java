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
package com.couchbase.client.java.util;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Utility classes for bootstrapping.
 *
 * @author Michael Nitschinger
 * @since 2.1.0
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Private
public class Bootstrap {

    private static Hashtable<String, String> DNS_ENV = new Hashtable<String, String>();
    private static final String DEFAULT_DNS_FACTORY = "com.sun.jndi.dns.DnsContextFactory";
    private static final String DEFAULT_DNS_PROVIDER = "dns:";

    static {
        DNS_ENV.put("java.naming.factory.initial", DEFAULT_DNS_FACTORY);
        DNS_ENV.put("java.naming.provider.url", DEFAULT_DNS_PROVIDER);
    }

    /**
     * The default DNS prefix for not encrypted connections.
     */
    private static final String DEFAULT_DNS_SERVICE = "_couchbase._tcp.";

    /**
     * The default DNS prefix for encrypted connections.
     */
    private static final String DEFAULT_DNS_SECURE_SERVICE = "_couchbases._tcp.";

    public static final void setDnsEnvParameter(String key, String value) {
        DNS_ENV.put(key, value);
    }

    private Bootstrap() {}

    /**
     * Fetch a bootstrap list from DNS SRV using default OS name resolution.
     *
     * @param serviceName the DNS SRV locator.
     * @param full if the service name is the full one or needs to be enriched by the couchbase prefixes.
     * @param secure if the secure service prefix should be used.
     * @return a list of DNS SRV records.
     * @throws NamingException if something goes wrong during the load process.
     */
    public static List<String> fromDnsSrv(final String serviceName, boolean full, boolean secure) throws NamingException {
        return fromDnsSrv(serviceName, full, secure, null);
    }

    /**
     * Fetch a bootstrap list from DNS SRV using a specific nameserver IP.
     *
     * @param serviceName the DNS SRV locator.
     * @param full if the service name is the full one or needs to be enriched by the couchbase prefixes.
     * @param secure if the secure service prefix should be used.
     * @param nameServerIP an IPv4 for the name server to use for SRV resolution.
     * @return a list of DNS SRV records.
     * @throws NamingException if something goes wrong during the load process.
     */
    public static List<String> fromDnsSrv(final String serviceName, boolean full, boolean secure,
            String nameServerIP) throws NamingException {
        String fullService;
        if (full) {
            fullService = serviceName;
        } else {
            fullService = (secure ? DEFAULT_DNS_SECURE_SERVICE : DEFAULT_DNS_SERVICE) + serviceName;
        }

        DirContext ctx;
        if (nameServerIP == null || nameServerIP.isEmpty()) {
            ctx = new InitialDirContext(DNS_ENV);
        } else {
            Hashtable<String, String> finalEnv = new Hashtable<String, String>(DNS_ENV);
            finalEnv.put("java.naming.provider.url", "dns://" + nameServerIP);
            ctx = new InitialDirContext(finalEnv);
        }
        return loadDnsRecords(fullService, ctx);
    }

    /**
     * Helper method to load a list of DNS SRV records.
     *
     * @param serviceName the service to locate.
     * @param ctx the directory context to fetch from.
     * @return the list of dns records
     * @throws NamingException if something goes wrong during the load process.
     */
    static List<String> loadDnsRecords(final String serviceName, final DirContext ctx) throws NamingException {
        Attributes attrs = ctx.getAttributes(serviceName, new String[] { "SRV" });
        NamingEnumeration<?> servers = attrs.get("srv").getAll();
        List<String> records = new ArrayList<String>();
        while (servers.hasMore()) {
            DnsRecord record = DnsRecord.fromString((String) servers.next());
            records.add(record.getHost());
        }
        return records;
    }

    /**
     * Value class to represent a dns record loaded from a DNS SRV row.
     */
    static class DnsRecord {

        private final int priority;
        private final int weight;
        private final int port;
        private final String host;

        public DnsRecord(int priority, int weight, int port, String host) {
            this.priority = priority;
            this.weight = weight;
            this.port = port;
            this.host = host.replaceAll("\\.$", "");
        }

        public String getHost() {
            return host;
        }

        public static DnsRecord fromString(String input) {
            String[] splitted = input.split(" ");
            return new DnsRecord(
                Integer.parseInt(splitted[0]),
                Integer.parseInt(splitted[1]),
                Integer.parseInt(splitted[2]),
                splitted[3]
            );
        }

        @Override
        public String toString() {
            return "DnsRecord{" +
            "priority=" + priority +
            ", weight=" + weight +
            ", port=" + port +
            ", host='" + host + '\'' +
            '}';
        }
    }


}
