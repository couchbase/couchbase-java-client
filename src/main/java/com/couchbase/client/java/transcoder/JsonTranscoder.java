package com.couchbase.client.java.transcoder;

import com.couchbase.client.core.lang.Tuple;
import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.deps.com.fasterxml.jackson.core.*;
import com.couchbase.client.deps.com.fasterxml.jackson.databind.*;
import com.couchbase.client.deps.com.fasterxml.jackson.databind.module.SimpleModule;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;

import java.io.IOException;

public class JsonTranscoder extends AbstractTranscoder<JsonDocument, JsonObject> {

    private final ObjectMapper mapper;

    public JsonTranscoder() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("JsonValueModule",
            new Version(1, 0, 0, null, null, null));
        module.addSerializer(JsonObject.class, new JsonObjectSerializer());
        module.addSerializer(JsonArray.class, new JsonArraySerializer());
        module.addDeserializer(JsonObject.class, new JsonObjectDeserializer());
        module.addDeserializer(JsonArray.class, new JsonArrayDeserializer());
        mapper.registerModule(module);
    }

    @Override
    public Class<JsonDocument> documentType() {
        return JsonDocument.class;
    }

    @Override
    protected Tuple2<ByteBuf, Integer> doEncode(JsonDocument document) throws Exception {
        String content = jsonObjectToString(document.content());
        int flags = 0;
        return Tuple.create(Unpooled.copiedBuffer(content, CharsetUtil.UTF_8), flags);
    }

    @Override
    protected JsonDocument doDecode(String id, ByteBuf content, long cas, int expiry, int flags, ResponseStatus status)
        throws Exception {
        JsonObject converted = stringToJsonObject(content.toString(CharsetUtil.UTF_8));
        content.release();
        return newDocument(id, expiry, converted, cas);
    }

    @Override
    public JsonDocument newDocument(String id, int expiry, JsonObject content, long cas) {
        return JsonDocument.create(id, expiry, content, cas);
    }

    public String jsonObjectToString(JsonObject input) throws Exception {
        return mapper.writeValueAsString(input);
    }

    public JsonObject stringToJsonObject(String input) throws Exception {
        return mapper.readValue(input, JsonObject.class);
    }

    public JsonArray stringTojsonArray(String input) throws Exception {
        return mapper.readValue(input, JsonArray.class);
    }

    public JsonObject byteBufToJsonObject(ByteBuf input) throws Exception {
        return stringToJsonObject(input.toString(CharsetUtil.UTF_8));
    }

    /**
     *
     */
    static class JsonObjectSerializer extends JsonSerializer<JsonObject> {
        @Override
        public void serialize(JsonObject value, JsonGenerator jgen,
                              SerializerProvider provider) throws IOException {
            jgen.writeObject(value.toMap());
        }
    }

    /**
     *
     */
    static class JsonArraySerializer extends JsonSerializer<JsonArray> {
        @Override
        public void serialize(JsonArray value, JsonGenerator jgen,
                              SerializerProvider provider) throws IOException {
            jgen.writeObject(value.toList());
        }
    }

    static abstract class AbstractJsonValueDeserializer<T> extends JsonDeserializer<T> {
        protected JsonObject decodeObject(final JsonParser parser, final JsonObject target) throws IOException {
            JsonToken current = parser.nextToken();
            String field = null;
            while(current != null && current != JsonToken.END_OBJECT) {
                if (current == JsonToken.START_OBJECT) {
                    target.put(field, decodeObject(parser, JsonObject.empty()));
                } else if (current == JsonToken.START_ARRAY) {
                    target.put(field, decodeArray(parser, JsonArray.empty()));
                } else if (current == JsonToken.FIELD_NAME) {
                    field = parser.getCurrentName();
                } else {
                    switch(current) {
                        case VALUE_TRUE:
                        case VALUE_FALSE:
                            target.put(field, parser.getValueAsBoolean());
                            break;
                        case VALUE_STRING:
                            target.put(field, parser.getValueAsString());
                            break;
                        case VALUE_NUMBER_INT:
                            try {
                                target.put(field, parser.getValueAsInt());
                            } catch (final JsonParseException e) {
                                target.put(field, parser.getValueAsLong());
                            }
                            break;
                        case VALUE_NUMBER_FLOAT:
                            target.put(field, parser.getValueAsDouble());
                            break;
                        case VALUE_NULL:
                            target.put(field, (JsonObject) null);
                            break;
                        default:
                            throw new IllegalStateException("Could not decode JSON token: " + current);
                    }
                }

                current = parser.nextToken();
            }
            return target;
        }

        protected JsonArray decodeArray(final JsonParser parser, final JsonArray target) throws IOException {
            JsonToken current = parser.nextToken();
            while (current != null && current != JsonToken.END_ARRAY) {
                if (current == JsonToken.START_OBJECT) {
                    target.add(decodeObject(parser, JsonObject.empty()));
                } else if (current == JsonToken.START_ARRAY) {
                    target.add(decodeArray(parser, JsonArray.empty()));
                } else {
                    switch(current) {
                        case VALUE_TRUE:
                        case VALUE_FALSE:
                            target.add(parser.getValueAsBoolean());
                            break;
                        case VALUE_STRING:
                            target.add(parser.getValueAsString());
                            break;
                        case VALUE_NUMBER_INT:
                            try {
                                target.add(parser.getValueAsInt());
                            } catch (final JsonParseException e) {
                                target.add(parser.getValueAsLong());
                            }
                            break;
                        case VALUE_NUMBER_FLOAT:
                            target.add(parser.getValueAsDouble());
                            break;
                        case VALUE_NULL:
                            target.add((JsonObject) null);
                            break;
                        default:
                            throw new IllegalStateException("Could not decode JSON token.");
                    }
                }

                current = parser.nextToken();
            }
            return target;
        }
    }

    static class JsonObjectDeserializer extends AbstractJsonValueDeserializer<JsonObject> {
        @Override
        public JsonObject deserialize(JsonParser jp, DeserializationContext ctx)
            throws IOException {
            if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
                return decodeObject(jp, JsonObject.empty());
            } else {
                throw new IllegalStateException("Expecting Object as root level object, " +
                    "was: " + jp.getCurrentToken());
            }
        }
    }

    static class JsonArrayDeserializer extends AbstractJsonValueDeserializer<JsonArray> {
        @Override
        public JsonArray deserialize(JsonParser jp, DeserializationContext ctx)
            throws IOException {
            if (jp.getCurrentToken() == JsonToken.START_ARRAY) {
                return decodeArray(jp, JsonArray.empty());
            } else {
                throw new IllegalStateException("Expecting Array as root level object, " +
                    "was: " + jp.getCurrentToken());
            }
        }
    }


}
