/*
 * Copyright (C) 2016 Couchbase, Inc.
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

package com.couchbase.client.java.document;

import static org.junit.Assert.assertEquals;

import com.couchbase.client.java.document.subdoc.ExtendDirection;
import com.couchbase.client.java.document.subdoc.LookupSpec;
import com.couchbase.client.java.document.subdoc.MutationSpec;
import org.junit.Test;

/**
 * Simple unit tests around the subdocument feature.
 *
 * @author Simon Baslé
 */
public class SubDocumentTest {

    @Test
    public void testLookupSpecToString() {
        LookupSpec spec1 = LookupSpec.exists("some/path/\"e\"");
        LookupSpec spec2 = LookupSpec.get("some/path/\"e\"");

        assertEquals("{EXIST:some/path/\"e\"}", spec1.toString());
        assertEquals("{GET:some/path/\"e\"}", spec2.toString());
    }

    @Test
    public void testMutationSpecToString() {
        MutationSpec spec1 = MutationSpec.insert("some/path/\"e\"", "toto", false);
        MutationSpec spec2 = MutationSpec.addUnique("some/path/\"e\"", "toto", true);
        MutationSpec spec3 = MutationSpec.extend("path", "toto", ExtendDirection.BACK, false);
        MutationSpec spec4 = MutationSpec.extend("path", "toto", ExtendDirection.FRONT, true);

        assertEquals("{DICT_ADD:some/path/\"e\"}", spec1.toString());
        assertEquals("{ARRAY_ADD_UNIQUE, createParents:some/path/\"e\"}", spec2.toString());
        assertEquals("{ARRAY_PUSH_LAST:path}", spec3.toString());
        assertEquals("{ARRAY_PUSH_FIRST, createParents:path}", spec4.toString());
    }
}
