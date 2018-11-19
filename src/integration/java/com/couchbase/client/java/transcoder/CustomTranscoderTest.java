/*
 * Copyright (c) 2017 Couchbase, Inc.
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

package com.couchbase.client.java.transcoder;

import com.couchbase.client.core.lang.Tuple;
import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.AbstractDocument;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.StringDocument;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.util.CouchbaseTestContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

/**
 * Exercises the registration and invocation of custom document transcoders.
 */
public class CustomTranscoderTest {

    private static CouchbaseTestContext ctx;

    private static final List<Transcoder<? extends Document, ?>> customTranscoders =
            Arrays.<Transcoder<? extends Document, ?>>asList(
                    new UpperCasingStringTranscoder(),
                    new ReversingTestTranscoder());

    @BeforeClass
    public static void connect() throws Exception {
        assumeFalse(CouchbaseTestContext.isMockEnabled());

        ctx = CouchbaseTestContext.builder()
                .adhoc(true)
                .build();

        // The bucket managed by the test context was opened without any transcoders.
        // Need to close it so the cached bucket is not returned by subsequent openBucket calls
        // that specify custom transcoders. See JCBC-1132 (Bucket cache ignores custom transcoders).
        ctx.bucket().close();
    }

    @AfterClass
    public static void disconnect() throws Exception {
        if (ctx != null) {
            ctx.destroyBucketAndDisconnect();
        }
    }

    private Bucket openBucket(List<Transcoder<? extends Document, ?>> transcoders) {
        return ctx.cluster().openBucket(ctx.bucketName(), transcoders);
    }

    private static void assertValueAfterRoundTripEquals(Bucket bucket, Document<String> doc, String expected) {
        bucket.upsert(doc);
        assertEquals(expected, bucket.get(doc).content());
    }

    private static void assertStringDocumentIsUpperCased(Bucket bucket) {
        assertValueAfterRoundTripEquals(bucket, StringDocument.create("id", "hello world"), "HELLO WORLD");
    }

    private static void assertTestDocumentIsReversed(Bucket bucket) {
        assertValueAfterRoundTripEquals(bucket, TestDocument.create("id", "hello world"), "dlrow olleh");
    }

    @Test
    public void canReplaceDefaultTranscoder() throws Exception {
        assertStringDocumentIsUpperCased(openBucket(customTranscoders));
    }

    @Test
    public void canAddCustomTranscoder() throws Exception {
        assertTestDocumentIsReversed(openBucket(customTranscoders));
    }

    /**
     * Custom document type to trigger the use of {@link ReversingTestTranscoder}.
     */
    private static class TestDocument extends AbstractDocument<String> {
        TestDocument(String id, int expiry, String content, long cas, MutationToken mutationToken) {
            super(id, expiry, content, cas, mutationToken);
        }

        private static TestDocument create(String id, String content) {
            return new TestDocument(id, 0, content, 0, null);
        }
    }

    /**
     * For testing whether we can add a new customBucket transcoder.
     * Transforms the content during encoding so we know it was registered.
     */
    private static class ReversingTestTranscoder extends AbstractTranscoder<TestDocument, String> {
        @Override
        protected TestDocument doDecode(String id, ByteBuf content, long cas, int expiry, int flags,
                                        ResponseStatus status) throws Exception {
            if (!TranscoderUtils.hasStringFlags(flags)) {
                throw new TranscodingException("Flags (0x" + Integer.toHexString(flags) + ") indicate non-String document for "
                        + "id " + id + ", could not decode.");
            }
            return newDocument(id, expiry, content.toString(CharsetUtil.UTF_8), cas);
        }

        @Override
        protected Tuple2<ByteBuf, Integer> doEncode(TestDocument document) throws Exception {
            String transformedContent = new StringBuilder(document.content()).reverse().toString();
            return Tuple.create(
                    TranscoderUtils.encodeStringAsUtf8(transformedContent),
                    TranscoderUtils.STRING_COMMON_FLAGS
            );
        }

        @Override
        public TestDocument newDocument(String id, int expiry, String content, long cas) {
            return new TestDocument(id, expiry, content, cas, null);
        }

        @Override
        public TestDocument newDocument(String id, int expiry, String content, long cas, MutationToken mutationToken) {
            return new TestDocument(id, expiry, content, cas, mutationToken);
        }

        @Override
        public Class<TestDocument> documentType() {
            return TestDocument.class;
        }
    }

    /**
     * For testing whether we can override a default transcoder.
     * Transforms the content during encoding so we know it was registered.
     */
    private static class UpperCasingStringTranscoder extends StringTranscoder {
        @Override
        protected Tuple2<ByteBuf, Integer> doEncode(StringDocument document) throws Exception {
            String transformedContent = document.content().toUpperCase(Locale.ROOT);
            return super.doEncode(StringDocument.from(document, document.id(), transformedContent));
        }
    }
}
