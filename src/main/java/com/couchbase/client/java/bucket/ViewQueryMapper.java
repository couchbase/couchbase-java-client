package com.couchbase.client.java.bucket;

import com.couchbase.client.core.message.view.ViewQueryResponse;
import com.couchbase.client.java.convert.Converter;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
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
        Converter<?, ?> converter = converters.get(JsonDocument.class);
        MarkersProcessor processor = new MarkersProcessor();
        response.content().forEachByte(processor);
        List<Integer> markers = processor.markers();
        List<JsonObject> objects = new ArrayList<JsonObject>();
        for (Integer marker : markers) {
            ByteBuf chunk = response.content().readBytes(marker - response.content().readerIndex());
            chunk.readerIndex(chunk.readerIndex()+1);
            objects.add((JsonObject) converter.decode(chunk));
        }
        return Observable.from(objects);
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
