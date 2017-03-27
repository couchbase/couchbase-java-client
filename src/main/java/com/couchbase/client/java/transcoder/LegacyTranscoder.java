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

package com.couchbase.client.java.transcoder;

import com.couchbase.client.core.lang.Tuple;
import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.document.LegacyDocument;

import java.io.*;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A {@link Transcoder} which mimics the behavior of the Java SDK 1.* series for compatibility.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class LegacyTranscoder extends AbstractTranscoder<LegacyDocument, Object> {

    /**
     * The logger used.
     */
    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(LegacyTranscoder.class);

    public static final int DEFAULT_COMPRESSION_THRESHOLD = 16384;

    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^-?\\d+$");

    // General flags
    static final int SERIALIZED = 1;
    static final int COMPRESSED = 2;

    // Special flags for specially handled types.
    private static final int SPECIAL_MASK = 0xff00;
    static final int SPECIAL_BOOLEAN = (1 << 8);
    static final int SPECIAL_INT = (2 << 8);
    static final int SPECIAL_LONG = (3 << 8);
    static final int SPECIAL_DATE = (4 << 8);
    static final int SPECIAL_BYTE = (5 << 8);
    static final int SPECIAL_FLOAT = (6 << 8);
    static final int SPECIAL_DOUBLE = (7 << 8);
    static final int SPECIAL_BYTEARRAY = (8 << 8);

    private final int compressionThreshold;

    public LegacyTranscoder() {
        this(DEFAULT_COMPRESSION_THRESHOLD);
    }

    public LegacyTranscoder(int compressionThreshold) {
        this.compressionThreshold = compressionThreshold;
    }

    @Override
    public Class<LegacyDocument> documentType() {
        return LegacyDocument.class;
    }

    @Override
    protected LegacyDocument doDecode(String id, ByteBuf content, long cas, int expiry, int flags, ResponseStatus status)
        throws Exception {
        byte[] data = new byte[content.readableBytes()];
        content.readBytes(data);
        Object decoded = null;
        if ((flags & COMPRESSED) != 0) {
            data = decompress(data);
        }
        int maskedFlags = flags & SPECIAL_MASK;
        if ((flags & SERIALIZED) != 0 && data != null) {
            decoded = deserialize(data);
        } else if (maskedFlags != 0 && data != null) {
            switch(maskedFlags) {
                case SPECIAL_BOOLEAN:
                    decoded = data[0] == '1';
                    break;
                case SPECIAL_INT:
                    decoded = (int) decodeLong(data);
                    break;
                case SPECIAL_LONG:
                    decoded = decodeLong(data);
                    break;
                case SPECIAL_DATE:
                    decoded = new Date(decodeLong(data));
                    break;
                case SPECIAL_BYTE:
                    decoded = data[0];
                    break;
                case SPECIAL_FLOAT:
                    decoded = Float.intBitsToFloat((int) decodeLong(data));
                    break;
                case SPECIAL_DOUBLE:
                    decoded = Double.longBitsToDouble(decodeLong(data));
                    break;
                case SPECIAL_BYTEARRAY:
                    decoded = data;
                    break;
                default:
                    LOGGER.warn("Undecodeable with flags %x", flags);
            }
        } else {
            decoded = new String(data, CharsetUtil.UTF_8);
        }
        return newDocument(id, expiry, decoded, cas);
    }

    @Override
    public LegacyDocument newDocument(String id, int expiry, Object content, long cas) {
        return LegacyDocument.create(id, expiry, content, cas);
    }

    @Override
    public LegacyDocument newDocument(String id, int expiry, Object content, long cas,
        MutationToken mutationToken) {
        return LegacyDocument.create(id, expiry, content, cas, mutationToken);
    }

    @Override
    protected Tuple2<ByteBuf, Integer> doEncode(LegacyDocument document)
        throws Exception {

        int flags = 0;
        Object content = document.content();

        boolean isJson = false;
        ByteBuf encoded;
        if (content instanceof String) {
            String c = (String) content;
            isJson = isJsonObject(c);
            encoded = TranscoderUtils.encodeStringAsUtf8(c);
        } else {
            encoded = Unpooled.buffer();

            if (content instanceof Long) {
                flags |= SPECIAL_LONG;
                encoded.writeBytes(encodeNum((Long) content, 8));
            } else if (content instanceof Integer) {
                flags |= SPECIAL_INT;
                encoded.writeBytes(encodeNum((Integer) content, 4));
            } else if (content instanceof Boolean) {
                flags |= SPECIAL_BOOLEAN;
                boolean b = (Boolean) content;
                encoded = Unpooled.buffer().writeByte(b ? '1' : '0');
            } else if (content instanceof Date) {
                flags |= SPECIAL_DATE;
                encoded.writeBytes(encodeNum(((Date) content).getTime(), 8));
            } else if (content instanceof Byte) {
                flags |= SPECIAL_BYTE;
                encoded.writeByte((Byte) content);
            } else if (content instanceof Float) {
                flags |= SPECIAL_FLOAT;
                encoded.writeBytes(encodeNum(Float.floatToRawIntBits((Float) content), 4));
            } else if (content instanceof Double) {
                flags |= SPECIAL_DOUBLE;
                encoded.writeBytes(encodeNum(Double.doubleToRawLongBits((Double) content), 8));
            } else if (content instanceof byte[]) {
                flags |= SPECIAL_BYTEARRAY;
                encoded.writeBytes((byte[]) content);
            } else {
                flags |= SERIALIZED;
                encoded.writeBytes(serialize(content));
            }
        }

        if (!isJson && encoded.readableBytes() >= compressionThreshold) {
            byte[] compressed = compress(encoded.copy().array());
            if (compressed.length < encoded.array().length) {
                encoded.clear().writeBytes(compressed);
                flags |= COMPRESSED;
            }
        }

        return Tuple.create(encoded, flags);
    }

    public static byte[] encodeNum(long l, int maxBytes) {
        byte[] rv = new byte[maxBytes];
        for (int i = 0; i < rv.length; i++) {
            int pos = rv.length - i - 1;
            rv[pos] = (byte) ((l >> (8 * i)) & 0xff);
        }
        int firstNon0 = 0;
        // Just looking for what we can reduce
        while (firstNon0 < rv.length && rv[firstNon0] == 0) {
            firstNon0++;
        }
        if (firstNon0 > 0) {
            byte[] tmp = new byte[rv.length - firstNon0];
            System.arraycopy(rv, firstNon0, tmp, 0, rv.length - firstNon0);
            rv = tmp;
        }
        return rv;
    }

    public static long decodeLong(byte[] b) {
        long rv = 0;
        for (byte i : b) {
            rv = (rv << 8) | (i < 0 ? 256 + i : i);
        }
        return rv;
    }

    private static byte[] serialize(final Object content) {
        if (content == null) {
            throw new NullPointerException("Can't serialize null");
        }
        byte[] rv=null;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream os = null;
        try {
            bos = new ByteArrayOutputStream();
            os = new ObjectOutputStream(bos);
            os.writeObject(content);
            os.close();
            bos.close();
            rv = bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("Non-serializable object", e);
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch(Exception ex) {
                LOGGER.error("Could not close output stream.", ex);
            }
            try {
                if (bos != null) {
                    bos.close();
                }
            } catch(Exception ex) {
                LOGGER.error("Could not close byte output stream.", ex);
            }
        }
        return rv;
    }

    protected Object deserialize(byte[] in) {
        Object rv=null;
        ByteArrayInputStream bis = null;
        ObjectInputStream is = null;
        try {
            if(in != null) {
                bis=new ByteArrayInputStream(in);
                is=new ObjectInputStream(bis);
                rv=is.readObject();
                is.close();
                bis.close();
            }
        } catch (IOException e) {
            LOGGER.warn("Caught IOException decoding %d bytes of data",
                in == null ? 0 : in.length, e);
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Caught CNFE decoding %d bytes of data",
                in == null ? 0 : in.length, e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch(Exception ex) {
                LOGGER.error("Could not close input stream.", ex);
            }
            try {
                if (bis != null) {
                    bis.close();
                }
            } catch(Exception ex) {
                LOGGER.error("Could not close byte input stream.", ex);
            }
        }
        return rv;
    }

    protected byte[] compress(byte[] in) {
        if (in == null) {
            throw new NullPointerException("Can't compress null");
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPOutputStream gz = null;
        try {
            gz = new GZIPOutputStream(bos);
            gz.write(in);
        } catch (IOException e) {
            throw new RuntimeException("IO exception compressing data", e);
        } finally {
            try {
                if (gz != null) {
                    gz.close();
                }
            } catch(Exception ex) {
                LOGGER.error("Could not close gzip output stream.", ex);
            }
            try {
                bos.close();
            } catch(Exception ex) {
                LOGGER.error("Could not close byte output stream.", ex);
            }
        }
        return bos.toByteArray();
    }

    protected byte[] decompress(byte[] in) {
        ByteArrayOutputStream bos = null;
        if(in != null) {
            ByteArrayInputStream bis = new ByteArrayInputStream(in);
            bos = new ByteArrayOutputStream();
            GZIPInputStream gis = null;
            try {
                gis = new GZIPInputStream(bis);

                byte[] buf = new byte[8192];
                int r = -1;
                while ((r = gis.read(buf)) > 0) {
                    bos.write(buf, 0, r);
                }
            } catch (IOException e) {
                LOGGER.error("Could not decompress data.", e);
                bos = null;
            } finally {
                try {
                    if (bos != null) {
                        bos.close();
                    }
                } catch(Exception ex) {
                    LOGGER.error("Could not close byte output stream.", ex);
                }
                try {
                    if (gis != null) {
                        gis.close();
                    }
                } catch(Exception ex) {
                    LOGGER.error("Could not close gzip input stream.", ex);
                }

                try {
                    bis.close();
                } catch(Exception ex) {
                    LOGGER.error("Could not close byte input stream.", ex);
                }
            }
        }
        return bos == null ? null : bos.toByteArray();
    }

    /**
     * check if its a json object or not.
     *
     * Note that this code is not bullet proof, but it is copied over from as-is
     * in the spymemcached project, since its intended to be compatible with it.
     */
    private static boolean isJsonObject(final String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }

        if (s.startsWith("{") || s.startsWith("[")
            || "true".equals(s) || "false".equals(s)
            || "null".equals(s) || DECIMAL_PATTERN.matcher(s).matches()) {
            return true;
        }

        return false;
    }
}
