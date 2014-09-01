package com.couchbase.client.java.transcoder;

import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.document.BinaryDocument;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BinaryTranscoderTest {

    private BinaryTranscoder converter;

    @Before
    public void setup() {
        converter = new BinaryTranscoder();
    }

    @Test
    public void shouldEncodeBinary() {
        BinaryDocument document = BinaryDocument.create("id", Unpooled.copiedBuffer("value", CharsetUtil.UTF_8));
        Tuple2<ByteBuf, Integer> encoded = converter.encode(document);

        assertEquals("value", encoded.value1().toString(CharsetUtil.UTF_8));
        assertEquals(TranscoderUtils.BINARY_COMPAT_FLAGS, (long) encoded.value2());
    }

    @Test
    public void shouldDecodeCommonBinary() {

    }

    @Test
    public void shouldDecodeLegacyBinary() {

    }

}
