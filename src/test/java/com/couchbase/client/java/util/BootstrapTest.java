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

import org.junit.Test;

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import java.util.List;

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
        List<String> strings = Bootstrap.fromDnsSrv(demoService, true, false);
        assertTrue(strings.size() > 0);
    }

}