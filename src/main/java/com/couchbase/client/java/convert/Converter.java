package com.couchbase.client.java.convert;

import io.netty.buffer.ByteBuf;

/**
 * Generic interface for document body converters.
 */
public interface Converter<D, T> {

  /**
   * Converts decode a {@link ByteBuf} into the target format.
   *
   * @param buffer the buffer encode convert decode.
   * @return the converted object.
   */
  T decode(ByteBuf buffer);

  /**
   * Converts decode the source format into a {@link ByteBuf}.
   *
   * @param content the source content.
   * @return the converted byte buffer.
   */
  ByteBuf encode(T content);

  /**
   * Creates a new and empty document.
   *
   * @return a new document.
   */
  D newDocument();

}
