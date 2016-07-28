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

import org.junit.Test;

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the functionality of the {@link Bootstrap} utility class.
 *
 * @author Michael Nitschinger
 * @since 2.1.0
 */
public class BootstrapTest {

    @Test
    public void shouldLoadDnsRecords() throws Exception {
        String service = "_couchbase._tcp.couchbase.com";
        BasicAttributes basicAttributes = new BasicAttributes(true);
        BasicAttribute basicAttribute = new BasicAttribute("SRV");
        basicAttribute.add("0 0 0 node2.couchbase.com.");
        basicAttribute.add("0 0 0 node1.couchbase.com.");
        basicAttribute.add("0 0 0 node3.couchbase.com.");
        basicAttribute.add("0 0 0 node4.couchbase.com.");
        basicAttributes.put(basicAttribute);
        DirContext mockedContext = mock(DirContext.class);
        when(mockedContext.getAttributes(service, new String[] { "SRV" }))
            .thenReturn(basicAttributes);

        List<String> records = Bootstrap.loadDnsRecords(service, mockedContext);
        assertEquals(4, records.size());
        assertEquals("node2.couchbase.com", records.get(0));
        assertEquals("node1.couchbase.com", records.get(1));
        assertEquals("node3.couchbase.com", records.get(2));
        assertEquals("node4.couchbase.com", records.get(3));
    }

    @Test
    public void shouldBootstrapFromDnsSrv() throws Exception {
        String demoService = "_xmpp-server._tcp.gmail.com";
        String publicNameServer = "8.8.8.8"; //google's public DNS
        List<String> strings = Bootstrap.fromDnsSrv(demoService, true, false, publicNameServer);
        assertTrue(strings.size() > 0);
    }

}