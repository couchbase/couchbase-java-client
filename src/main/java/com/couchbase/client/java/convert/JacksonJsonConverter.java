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
package com.couchbase.client.java.convert;

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

/**
 * Converter for {@link JsonObject}s.
 */
public class JacksonJsonConverter implements Converter<JsonDocument, JsonObject> {

  /**
   * The internal jackson object mapper.
   */
  private final ObjectMapper mapper;

  public JacksonJsonConverter() {
    mapper = new ObjectMapper();
    SimpleModule module = new SimpleModule("JsonValueModule",
      new Version(1, 0, 0, null, null, null));
    module.addSerializer(JsonObject.class, new JsonObjectSerializer());
    module.addSerializer(JsonArray.class, new JsonArraySerializer());
    module.addDeserializer(JsonObject.class, new JsonObjectDeserializer());
    mapper.registerModule(module);
  }

  @Override
  public JsonObject decode(ByteBuf buffer) {
        return decode(buffer.toString(CharsetUtil.UTF_8));
  }

  public JsonObject decode(String buffer) {
      try {
          return mapper.readValue(buffer, JsonObject.class);
      } catch (IOException e) {
          throw new IllegalStateException(e);
      }
  }

  @Override
  public ByteBuf encode(JsonObject content) {
      return Unpooled.copiedBuffer(encodeToString(content), CharsetUtil.UTF_8);
  }

  public String encodeToString(JsonObject content) {
      try {
          return mapper.writeValueAsString(content);
      } catch (JsonProcessingException e) {
          throw new IllegalStateException(e);
      }
  }

    @Override
    public JsonDocument newDocument(String id, JsonObject content, long cas, int expiry, ResponseStatus status) {
        return JsonDocument.create(id, content, cas, expiry, status);
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

  /**
   *
   */
  static class JsonObjectDeserializer extends JsonDeserializer<JsonObject> {
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

    private JsonObject decodeObject(final JsonParser parser, final JsonObject target) throws IOException {
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

    private JsonArray decodeArray(final JsonParser parser, final JsonArray target) throws IOException {
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

}
