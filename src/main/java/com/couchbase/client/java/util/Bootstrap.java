/**
 * Copyright (C) 2015 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
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
@InterfaceStability.Experimental
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

    /**
     * Fetch a bootstrap list from DNS SRV.
     *
     * @param serviceName the DNS SRV locator.
     * @param full if the service name is the full one or needs to be enriched by the couchbase prefixes.
     * @param secure if the secure service prefix should be used.
     * @return a list of DNS SRV records.
     * @throws NamingException if something goes wrong during the load process.
     */
    public static List<String> fromDnsSrv(final String serviceName, boolean full, boolean secure) throws NamingException {
        String fullService;
        if (full) {
            fullService = serviceName;
        } else {
            fullService = (secure ? DEFAULT_DNS_SECURE_SERVICE : DEFAULT_DNS_SERVICE) + serviceName;
        }
        DirContext ctx = new InitialDirContext(DNS_ENV);
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
