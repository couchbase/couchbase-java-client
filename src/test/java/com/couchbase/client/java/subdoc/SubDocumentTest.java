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

package com.couchbase.client.java.subdoc;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.subdoc.multi.Lookup;
import com.couchbase.client.core.message.kv.subdoc.multi.Mutation;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.error.subdoc.PathMismatchException;
import org.junit.Test;

/**
 * Simple unit tests around the subdocument feature.
 *
 * @author Simon Baslé
 */
public class SubDocumentTest {

    @Test
    public void testLookupSpecToString() {
        LookupSpec spec1 = new LookupSpec(Lookup.EXIST, "some/path/\"e\"");
        LookupSpec spec2 = new LookupSpec(Lookup.GET, "some/path/\"e\"");

        assertEquals("{EXIST:some/path/\"e\"}", spec1.toString());
        assertEquals("{GET:some/path/\"e\"}", spec2.toString());
    }

    @Test
    public void testMutationSpecToString() {
        MutationSpec spec1 = new MutationSpec(Mutation.DICT_ADD, "some/path/\"e\"", "toto", false);
        MutationSpec spec2 = new MutationSpec(Mutation.ARRAY_ADD_UNIQUE, "some/path/\"e\"", "toto", true);
        MutationSpec spec3 = new MutationSpec(Mutation.ARRAY_PUSH_LAST, "path", "toto", false);
        MutationSpec spec4 = new MutationSpec(Mutation.ARRAY_PUSH_FIRST, "path", "toto", true);

        assertEquals("{DICT_ADD:some/path/\"e\"}", spec1.toString());
        assertEquals("{ARRAY_ADD_UNIQUE, createParents:some/path/\"e\"}", spec2.toString());
        assertEquals("{ARRAY_PUSH_LAST:path}", spec3.toString());
        assertEquals("{ARRAY_PUSH_FIRST, createParents:path}", spec4.toString());
    }

    @Test
    public void testEmptyDocumentFragmentToString() {
        DocumentFragment fragment = new DocumentFragment("id", 123L, null, Collections.emptyList());
        String expected = "DocumentFragment{id='id', cas=123, mutationToken=null}";

        assertEquals(expected, fragment.toString());
    }

    @Test
    public void testNullListDocumentFragmentToString() {
        DocumentFragment fragment = new DocumentFragment("id", 123L, null, null);
        String expected = "DocumentFragment{id='id', cas=123, mutationToken=null}";

        assertEquals(expected, fragment.toString());
    }

    @Test
    public void testDocumentFragmentToString() {
        SubdocOperationResult<Lookup> result1 = SubdocOperationResult.createError("path", Lookup.GET, ResponseStatus.SUBDOC_PATH_MISMATCH,
                new PathMismatchException("id", "path"));
        SubdocOperationResult<Lookup> result2 = SubdocOperationResult
                .createResult("path", Lookup.GET, ResponseStatus.SUCCESS, "foo");
        SubdocOperationResult<Lookup> result3 = SubdocOperationResult.createFatal("path", Lookup.EXIST, new TranscodingException("test"));

        DocumentFragment<Lookup> fragment = new DocumentFragment<Lookup>("id", 123L, null, Arrays.asList(result1, result2, result3));
        String expected = "DocumentFragment{id='id', cas=123, mutationToken=null}[GET(path){error=SUBDOC_PATH_MISMATCH}, " +
                "GET(path){value=foo}, " +
                "EXIST(path){fatal=com.couchbase.client.java.error.TranscodingException: test}]";

        assertEquals(expected, fragment.toString());
    }
}
