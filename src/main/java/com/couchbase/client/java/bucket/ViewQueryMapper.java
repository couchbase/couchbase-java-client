/**
 * Copyright (C) 2014 Couchbase, Inc.
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
package com.couchbase.client.java.bucket;

import com.couchbase.client.core.message.view.ViewQueryResponse;
import com.couchbase.client.deps.io.netty.buffer.ByteBufProcessor;
import com.couchbase.client.java.convert.Converter;
import com.couchbase.client.java.document.json.JsonObject;
import rx.Observable;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ViewQueryMapper implements Func1<ViewQueryResponse, Observable<JsonObject>> {

    private final Map<Class<?>, Converter<?, ?>> converters;

    public ViewQueryMapper(Map<Class<?>, Converter<?, ?>> converters) {
        this.converters = converters;
    }

    @Override
    public Observable<JsonObject> call(ViewQueryResponse response) {
        /*Converter<?, ?> converter = converters.get(JsonDocument.class);
        MarkersProcessor processor = new MarkersProcessor();
        response.content().forEachByte(processor);
        List<Integer> markers = processor.markers();
        List<JsonObject> objects = new ArrayList<JsonObject>();
        for (Integer marker : markers) {
            ByteBuf chunk = response.content().readBytes(marker - response.content().readerIndex());
            chunk.readerIndex(chunk.readerIndex()+1);
            objects.add((JsonObject) converter.decode(chunk));
        }*/
        return null;
    }

    static class MarkersProcessor implements ByteBufProcessor {
        List<Integer> markers = new ArrayList<Integer>();
        private int depth;
        private byte open = '{';
        private byte close = '}';
        private int counter;
        @Override
        public boolean process(byte value) throws Exception {
            counter++;
            if (value == open) {
                depth++;
            }
            if (value == close) {
                depth--;
                if (depth == 0) {
                    markers.add(counter);
                }
            }
            return true;
        }

        public List<Integer> markers() {
            return markers;
        }
    }
}
