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

package com.couchbase.client.java.subdoc;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.subdoc.multi.Lookup;
import com.couchbase.client.core.message.kv.subdoc.multi.Mutation;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.error.subdoc.PathMismatchException;
import com.couchbase.client.java.error.subdoc.XattrOrderingException;
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

        assertEquals("{\"type\":DICT_ADD, \"path\":some/path/\"e\", \"createPath\":false, \"xattr\":false, \"expandMacros\":false}", spec1.toString());
        assertEquals("{\"type\":ARRAY_ADD_UNIQUE, \"path\":some/path/\"e\", \"createPath\":true, \"xattr\":false, \"expandMacros\":false}", spec2.toString());
        assertEquals("{\"type\":ARRAY_PUSH_LAST, \"path\":path, \"createPath\":false, \"xattr\":false, \"expandMacros\":false}", spec3.toString());
        assertEquals("{\"type\":ARRAY_PUSH_FIRST, \"path\":path, \"createPath\":true, \"xattr\":false, \"expandMacros\":false}", spec4.toString());
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

    @Test(expected = XattrOrderingException.class)
    public void shouldFailIfXattrLookupIsNotFirst() {
        new AsyncLookupInBuilder(null, null, null, null, "id")
            .get("foo")
            .get("bar", new SubdocOptionsBuilder().xattr(true))
            .execute();
    }

    @Test(expected = XattrOrderingException.class)
    public void shouldFailIfXattrMutateIsNotFirst() {
        new AsyncMutateInBuilder(null, null, null, null, "id")
            .remove("foo")
            .remove("bar", new SubdocOptionsBuilder().xattr(true))
            .execute();
    }
}
