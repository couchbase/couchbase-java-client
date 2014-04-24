package com.couchbase.client.java.convert;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

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
    try {
      return mapper.readValue(buffer.toString(CharsetUtil.UTF_8),
        JsonObject.class);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public ByteBuf encode(JsonObject content) {
    try {
      return Unpooled.copiedBuffer(mapper.writeValueAsString(content),
        CharsetUtil.UTF_8);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public JsonDocument newDocument() {
    return new JsonDocument();
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
