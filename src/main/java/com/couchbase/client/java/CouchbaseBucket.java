package com.couchbase.client.java;

import com.couchbase.client.core.cluster.Cluster;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.binary.*;
import com.couchbase.client.core.message.query.GenericQueryRequest;
import com.couchbase.client.core.message.query.GenericQueryResponse;
import com.couchbase.client.core.message.view.ViewQueryRequest;
import com.couchbase.client.core.message.view.ViewQueryResponse;
import com.couchbase.client.java.bucket.ViewQueryMapper;
import com.couchbase.client.java.convert.Converter;
import com.couchbase.client.java.convert.JacksonJsonConverter;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlRow;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewRow;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import rx.Observable;
import rx.functions.Func1;

import java.util.HashMap;
import java.util.Map;

public class CouchbaseBucket implements Bucket {

  private final String bucket;
  private final String password;
  private final Cluster core;
  private final Map<Class<?>, Converter<?, ?>> converters;

  public CouchbaseBucket(final Cluster core, final String name, final String password) {
    bucket = name;
    this.password = password;
    this.core = core;

    converters = new HashMap<Class<?>, Converter<?, ?>>();
    converters.put(JsonDocument.class, new JacksonJsonConverter());
  }

  @Override
  public Observable<JsonDocument> get(final String id) {
    return get(id, JsonDocument.class);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <D extends Document<?>> Observable<D> get(final String id, final Class<D> target) {
    return core.<GetResponse>send(new GetRequest(id, bucket)).map(new Func1<GetResponse, D>() {
        @Override
        public D call(final GetResponse response) {
          Converter<?, Object> converter = (Converter<?, Object>) converters.get(target);
          Object content = response.status() == ResponseStatus.SUCCESS ? converter.decode(response.content()) : null;
          return (D) converter.newDocument(id, content, response.cas(), 0, response.status());
        }
      }
    );
  }

  @Override
  @SuppressWarnings("unchecked")
  public <D extends Document<?>> Observable<D> insert(final D document) {
    final Converter<?, Object> converter = (Converter<?, Object>) converters.get(document.getClass());
    ByteBuf content = converter.encode(document.content());
    return core
      .<InsertResponse>send(new InsertRequest(document.id(), content, bucket))
      .map(new Func1<InsertResponse, D>() {
        @Override
        public D call(InsertResponse response) {
          return (D) converter.newDocument(document.id(), document.content(), response.cas(), document.expiry(),
              response.status());
        }
      });
  }

  @Override
  @SuppressWarnings("unchecked")
  public <D extends Document<?>> Observable<D> upsert(final D document) {
    final Converter<?, Object> converter = (Converter<?, Object>) converters.get(document.getClass());
    ByteBuf content = converter.encode(document.content());
    return core.<UpsertResponse>send(new UpsertRequest(document.id(), content, bucket))
      .map(new Func1<UpsertResponse, D>() {
        @Override
        public D call(UpsertResponse response) {
            return (D) converter.newDocument(document.id(), document.content(), response.cas(), document.expiry(),
                response.status());
        }
      });
  }

  @Override
  @SuppressWarnings("unchecked")
  public <D extends Document<?>> Observable<D> replace(final D document) {
    final Converter<?, Object> converter = (Converter<?, Object>) converters.get(document.getClass());
    ByteBuf content = converter.encode(document.content());
    return core.<ReplaceResponse>send(new ReplaceRequest(document.id(), content, bucket))
      .map(new Func1<ReplaceResponse, D>() {
        @Override
        public D call(ReplaceResponse response) {
            return (D) converter.newDocument(document.id(), document.content(), response.cas(), document.expiry(),
                response.status());
        }
      });
  }

  @Override
  @SuppressWarnings("unchecked")
  public <D extends Document<?>> Observable<D> remove(final D document) {
      final Converter<?, Object> converter = (Converter<?, Object>) converters.get(document.getClass());
    RemoveRequest request = new RemoveRequest(document.id(), document.cas(),
      bucket);
    return core.<RemoveResponse>send(request).map(new Func1<RemoveResponse, D>() {
      @Override
      public D call(RemoveResponse response) {
          return (D) converter.newDocument(document.id(), document.content(), document.cas(), document.expiry(),
              response.status());
      }
    });
  }

  @Override
  public Observable<JsonDocument> remove(final String id) {
    return remove(id, JsonDocument.class);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <D extends Document<?>> Observable<D> remove(final String id, final Class<D> target) {
    Converter<?, ?> converter = converters.get(target);
    return remove((D) converter.newDocument(id, null, 0, 0, null));
  }

  @Override
  public Observable<ViewRow> query(final ViewQuery query) {
    final ViewQueryRequest request = new ViewQueryRequest(query.design(), query.view(), query.development(),
        bucket, password);

    return core
        .<ViewQueryResponse>send(request)
        .flatMap(new ViewQueryMapper(converters))
        .map(new Func1<JsonObject, ViewRow>() {
            @Override
            public ViewRow call(JsonObject object) {
                return new ViewRow(object.getString("id"), object.getString("key"), object.get("value"));
            }
        }
    );
  }

    @Override
    public Observable<N1qlRow> query(N1qlQuery query) {
        final Converter<?, ?> converter = converters.get(JsonDocument.class);
        GenericQueryRequest request = new GenericQueryRequest(query.export(), bucket, password);
        return core
            .<GenericQueryResponse>send(request)
            .filter(new Func1<GenericQueryResponse, Boolean>() {
                @Override
                public Boolean call(GenericQueryResponse response) {
                    return response.content() != null;
                }
            })
            .map(new Func1<GenericQueryResponse, N1qlRow>() {
                @Override
                public N1qlRow call(GenericQueryResponse response) {
                    return new N1qlRow((JsonObject) converter.decode(Unpooled.copiedBuffer(response.content(), CharsetUtil.UTF_8)));
                }
            });
    }
}
