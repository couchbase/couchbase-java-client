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
package com.couchbase.client.java.transcoder;

import org.junit.Before;

/**
 * Tests which verify the functionality for the {@link JsonTranscoder}.
 */
public class JsonTranscoderTest {

  private JsonTranscoder converter;

  @Before
  public void setup() {
    converter = new JsonTranscoder();
  }
/*
  @Test
  public void shouldEncodeEmptyJsonObject() {
    JsonObject object = JsonObject.empty();
    ByteBuf buf = converter.encode(object);
    assertEquals("{}", buf.toString(CharsetUtil.UTF_8));
  }

  @Test
  public void shouldDecodeEmptyJsonObject() {
    ByteBuf buf = Unpooled.copiedBuffer("{}", CharsetUtil.UTF_8);
    JsonObject object = converter.decode(buf);
    assertTrue(object.isEmpty());
  }

  @Test
  public void shouldEncodeEmptyJsonArray() {
    JsonObject object = JsonObject.empty();
    object.put("array", JsonArray.empty());
    ByteBuf buf = converter.encode(object);
    assertEquals("{\"array\":[]}", buf.toString(CharsetUtil.UTF_8));
  }

  @Test
  public void shouldDecodeEmptyJsonArray() {
    ByteBuf buf = Unpooled.copiedBuffer("{\"array\":[]}", CharsetUtil.UTF_8);
    JsonObject object = converter.decode(buf);
    assertFalse(object.isEmpty());
    assertTrue(object.getArray("array") instanceof JsonArray);
    assertTrue(object.getArray("array").isEmpty());
  }

  @Test
  public void shouldEncodeSimpleJsonObject() {
    JsonObject object = JsonObject.empty();
    object.put("string", "Hello World");
    object.put("integer", 1);
    object.put("long", Long.MAX_VALUE);
    object.put("double", 11.3322);
    object.put("boolean", true);

    ByteBuf buf = converter.encode(object);
    String expected = "{\"integer\":1,\"string\":\"Hello World\",\"boolean\":" +
      "true,\"double\":11.3322,\"long\":9223372036854775807}";
    assertEquals(expected, buf.toString(CharsetUtil.UTF_8));
  }

  @Test
  public void shouldDecodeSimpleJsonObject() {
    String input = "{\"integer\":1,\"string\":\"Hello World\",\"boolean\":" +
      "true,\"double\":11.3322,\"long\":9223372036854775807}";
    ByteBuf buf = Unpooled.copiedBuffer(input, CharsetUtil.UTF_8);
    JsonObject object = converter.decode(buf);

    assertEquals(1, object.getInt("integer"));
    assertEquals("Hello World", object.getString("string"));
    assertEquals(Long.MAX_VALUE, object.getLong("long"));
    assertEquals(11.3322, object.getDouble("double"), 0);
    assertTrue(object.getBoolean("boolean"));
  }

  @Test
  public void shouldEncodeSimpleJsonArray() {
    JsonArray array = JsonArray.empty();
    array.add("Hello World");
    array.add(1);
    array.add(Long.MAX_VALUE);
    array.add(11.3322);
    array.add(false);

    ByteBuf buf = converter.encode(JsonObject.empty().put("array", array));
    String expected = "{\"array\":[\"Hello World\",1,9223372036854775807," +
      "11.3322,false]}";
    assertEquals(expected, buf.toString(CharsetUtil.UTF_8));
  }

  @Test
  public void shouldDecodeSimpleJsonArray() {
    String input = "{\"array\":[\"Hello World\",1,9223372036854775807," +
      "11.3322,false]}";
    ByteBuf buf = Unpooled.copiedBuffer(input, CharsetUtil.UTF_8);
    JsonObject object = converter.decode(buf);

    JsonArray array = object.getArray("array");
    assertEquals("Hello World", array.getString(0));
    assertEquals(1, array.getInt(1));
    assertEquals(Long.MAX_VALUE, array.getLong(2));
    assertEquals(11.3322, array.getDouble(3), 0);
    assertFalse(array.getBoolean(4));
  }

  @Test
  public void shouldEncodeNestedJsonObjects() {
    JsonObject inner = JsonObject.empty().put("foo", "bar");
    JsonObject object = JsonObject
      .empty()
      .put("object", JsonObject.empty().put("inner", inner));

    ByteBuf buf = converter.encode(object);
    String expected = "{\"object\":{\"inner\":{\"foo\":\"bar\"}}}";
    assertEquals(expected, buf.toString(CharsetUtil.UTF_8));
  }

  @Test
  public void shouldDecodeNestedJsonObjects() {
    String input = "{\"object\":{\"inner\":{\"foo\":\"bar\"}}}";
    ByteBuf buf = Unpooled.copiedBuffer(input, CharsetUtil.UTF_8);
    JsonObject object = converter.decode(buf);

    assertEquals(1, object.size());
    assertEquals(1, object.getObject("object").size());
    assertEquals("bar", object.getObject("object").getObject("inner")
      .get("foo"));
  }

  @Test
  public void shouldEncodeNestedJsonArrays() {
    ByteBuf buf = converter.encode(JsonObject.empty().put("inner", JsonArray
      .empty().add(JsonArray.empty())));
    String expected = "{\"inner\":[[]]}";
    assertEquals(expected, buf.toString(CharsetUtil.UTF_8));
  }

  @Test
  public void shouldDecodeNestedJsonArray() {
    String input = "{\"inner\":[[]]}";
    ByteBuf buf = Unpooled.copiedBuffer(input, CharsetUtil.UTF_8);
    JsonObject object = converter.decode(buf);

    assertEquals(1, object.size());
    assertEquals(1, object.getArray("inner").size());
    assertTrue(object.getArray("inner").getArray(0).isEmpty());
  }

  @Test
  public void shouldEncodeMixedNestedJsonValues() {
    JsonArray children = JsonArray.empty()
      .add(JsonObject.empty().put("name", "Jane Doe").put("age", 25))
      .add(JsonObject.empty().put("name", "Tom Doe").put("age", 13));

    JsonObject user = JsonObject.empty()
      .put("firstname", "John")
      .put("lastname", "Doe")
      .put("colors", JsonArray.empty().add("red").add("blue"))
      .put("children", children)
      .put("active", true);

    String expected = "{\"colors\":[\"red\",\"blue\"],\"active\":true," +
      "\"children\":[{\"age\":25,\"name\":\"Jane Doe\"},{\"age\":13,\"name\":" +
      "\"Tom Doe\"}],\"lastname\":\"Doe\",\"firstname\":\"John\"}";
    ByteBuf buf = converter.encode(user);
    assertEquals(expected, buf.toString(CharsetUtil.UTF_8));
  }

  @Test
  public void shouldDecodeMixedNestedJsonValues() {
    String input = "{\"colors\":[\"red\",\"blue\"],\"active\":true," +
      "\"children\":[{\"age\":25,\"name\":\"Jane Doe\"},{\"age\":13,\"name\":" +
      "\"Tom Doe\"}],\"lastname\":\"Doe\",\"firstname\":\"John\"}";
    ByteBuf buf = Unpooled.copiedBuffer(input, CharsetUtil.UTF_8);
    JsonObject user = converter.decode(buf);

    assertEquals("John", user.getString("firstname"));
    assertEquals("Doe", user.getString("lastname"));
    assertEquals(2, user.getArray("colors").size());
    assertEquals("red", user.getArray("colors").get(0));
    assertEquals("blue", user.getArray("colors").get(1));
    assertEquals(true, user.getBoolean("active"));

    JsonObject child0 = user.getArray("children").getObject(0);
    JsonObject child1 = user.getArray("children").getObject(1);
    assertEquals("Jane Doe", child0.getString("name"));
    assertEquals("Tom Doe", child1.getString("name"));
  }*/

}
