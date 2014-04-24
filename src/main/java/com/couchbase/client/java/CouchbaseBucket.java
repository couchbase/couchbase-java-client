package com.couchbase.client.java;

import com.couchbase.client.core.cluster.Cluster;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.binary.GetRequest;
import com.couchbase.client.core.message.binary.GetResponse;
import com.couchbase.client.core.message.binary.InsertRequest;
import com.couchbase.client.core.message.binary.InsertResponse;
import com.couchbase.client.core.message.binary.UpsertRequest;
import com.couchbase.client.core.message.binary.UpsertResponse;
import com.couchbase.client.java.convert.Converter;
import com.couchbase.client.java.convert.JacksonJsonConverter;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import io.netty.buffer.ByteBuf;
import rx.Observable;
import rx.functions.Func1;

import java.util.HashMap;
import java.util.Map;

public class CouchbaseBucket implements Bucket {

  private final String bucket;
  private final Cluster core;

  private final Map<Class<?>, Converter> converters;

  public CouchbaseBucket(final Cluster core, final String name) {
    bucket = name;
    this.core = core;

    converters = new HashMap<Class<?>, Converter>();
    converters.put(JsonDocument.class, new JacksonJsonConverter());
  }

  @Override
  public Observable<JsonDocument> get(final String id) {
    return get(id, JsonDocument.class);
  }

  @Override
  public <D extends Document> Observable<D> get(final String id, final Class<D> target) {
    return core.<GetResponse>send(new GetRequest(id, bucket))
      .map(new Func1<GetResponse, D>() {
        @Override
        public D call(final GetResponse response) {
          Converter converter = converters.get(target);
          D document = (D) converter.newDocument();
          Object content = response.status() == ResponseStatus.OK
            ? converter.decode(response.content()) : null;
          document.id(id);
          document.content(content);
          document.cas(response.cas());
          document.expiry(0);
          return document;
        }
      }
    );
  }

  @Override
  public <D extends Document> Observable<D> insert(final D document) {
    Converter converter = converters.get(document.getClass());
    ByteBuf content = converter.encode(document.content());
    return core
      .<InsertResponse>send(new InsertRequest(document.id(), content, bucket))
      .map(new Func1<InsertResponse, D>() {
        @Override
        public D call(InsertResponse response) {
          document.cas(response.cas());
          return document;
        }
      });
  }

  @Override
  public <D extends Document> Observable<D> upsert(final  D document) {
    Converter converter = converters.get(document.getClass());
    ByteBuf content = converter.encode(document.content());
    return core
      .<UpsertResponse>send(new UpsertRequest(document.id(), content, bucket))
      .map(new Func1<UpsertResponse, D>() {
        @Override
        public D call(UpsertResponse response) {
          document.cas(response.cas());
          return document;
        }
      });
  }
}
