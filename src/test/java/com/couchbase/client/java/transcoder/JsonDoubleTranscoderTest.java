package com.couchbase.client.java.transcoder;

import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.document.JsonDoubleDocument;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonDoubleTranscoderTest {

    private JsonDoubleTranscoder converter;

    @Before
    public void setup() {
        converter = new JsonDoubleTranscoder();
    }

    @Test
    public void shouldEncodeDouble() {
        JsonDoubleDocument document = JsonDoubleDocument.create("id", Double.MAX_VALUE);
        Tuple2<ByteBuf, Integer> encoded = converter.encode(document);

        assertEquals("1.7976931348623157E308", encoded.value1().toString(CharsetUtil.UTF_8));
        assertEquals(TranscoderUtils.JSON_COMPAT_FLAGS, (long) encoded.value2());
    }

    @Test
    public void shouldDecodeCommonFlagsDouble() {
        ByteBuf content = Unpooled.copiedBuffer("1.7976931348623157E308", CharsetUtil.UTF_8);
        JsonDoubleDocument decoded = converter.decode("id", content, 0, 0, TranscoderUtils.JSON_COMMON_FLAGS,
            ResponseStatus.SUCCESS);

        assertEquals(Double.MAX_VALUE, decoded.content(), 0);
    }

    @Test
    public void shouldDecodeLegacyDouble() {
        byte[] bytes = LegacyTranscoder.encodeNum(Double.doubleToRawLongBits(Double.MAX_VALUE), 8);
        ByteBuf content = Unpooled.buffer().writeBytes(bytes);
        JsonDoubleDocument decoded = converter.decode("id", content, 0, 0, 7 << 8,
            ResponseStatus.SUCCESS);

        assertEquals(Double.MAX_VALUE, decoded.content(), 0);
    }

    @Test
    public void shouldDecodeLegacyFloat() {
        byte[] bytes = LegacyTranscoder.encodeNum(Float.floatToRawIntBits(Float.MAX_VALUE), 4);
        ByteBuf content = Unpooled.buffer().writeBytes(bytes);
        JsonDoubleDocument decoded = converter.decode("id", content, 0, 0, 6 << 8,
            ResponseStatus.SUCCESS);

        assertEquals(Float.MAX_VALUE, decoded.content(), 0);
    }

}