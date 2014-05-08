package com.couchbase.client.java;

import com.couchbase.client.core.cluster.Cluster;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.binary.GetRequest;
import com.couchbase.client.core.message.binary.GetResponse;
import com.couchbase.client.core.message.binary.InsertRequest;
import com.couchbase.client.core.message.binary.InsertResponse;
import com.couchbase.client.core.message.binary.RemoveRequest;
import com.couchbase.client.core.message.binary.RemoveResponse;
import com.couchbase.client.core.message.binary.ReplaceRequest;
import com.couchbase.client.core.message.binary.ReplaceResponse;
import com.couchbase.client.core.message.binary.UpsertRequest;
import com.couchbase.client.core.message.binary.UpsertResponse;
import com.couchbase.client.core.message.view.ViewQueryRequest;
import com.couchbase.client.core.message.view.ViewQueryResponse;
import com.couchbase.client.java.convert.Converter;
import com.couchbase.client.java.convert.JacksonJsonConverter;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewRow;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import rx.Observable;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    final ViewQueryRequest request = new ViewQueryRequest(query.design(), query.view(), query.development(), bucket, password);

    return core.<ViewQueryResponse>send(request).flatMap(new Func1<ViewQueryResponse, Observable<JsonObject>>() {
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

        class MarkersProcessor implements ByteBufProcessor {
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
    }).map(new Func1<JsonObject, ViewRow>() {
        @Override
        public ViewRow call(JsonObject object) {
            return new ViewRow(object.getString("id"), object.getString("key"), object.get("value"));
        }
    });
  }

}
