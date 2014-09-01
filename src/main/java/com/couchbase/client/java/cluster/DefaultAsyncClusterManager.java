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
package com.couchbase.client.java.cluster;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.message.config.*;
import com.couchbase.client.core.message.internal.AddNodeRequest;
import com.couchbase.client.core.message.internal.AddNodeResponse;
import com.couchbase.client.core.message.internal.AddServiceRequest;
import com.couchbase.client.core.message.internal.AddServiceResponse;
import com.couchbase.client.core.service.ServiceType;
import com.couchbase.client.java.ConnectionString;
import com.couchbase.client.java.CouchbaseAsyncBucket;
import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.error.BucketAlreadyExistsException;
import com.couchbase.client.java.error.BucketDoesNotExistException;
import com.couchbase.client.java.error.TranscodingException;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class DefaultAsyncClusterManager implements AsyncClusterManager {

    private final ClusterFacade core;
    private final String username;
    private final String password;
    private final CouchbaseEnvironment environment;
    private final ConnectionString connectionString;

    DefaultAsyncClusterManager(final String username, final String password, final ConnectionString connectionString,
                               final CouchbaseEnvironment environment, final ClusterFacade core) {
        this.username = username;
        this.password = password;
        this.core = core;
        this.environment = environment;
        this.connectionString = connectionString;
    }

    public static DefaultAsyncClusterManager create(final String username, final String password,
        final ConnectionString connectionString, final CouchbaseEnvironment environment, final ClusterFacade core) {
        return new DefaultAsyncClusterManager(username, password, connectionString, environment, core);
    }

    @Override
    public Observable<ClusterInfo> info() {
        return
            ensureServiceEnabled()
            .flatMap(new Func1<Boolean, Observable<ClusterConfigResponse>>() {
                @Override
                public Observable<ClusterConfigResponse> call(Boolean aBoolean) {
                    return core.send(new ClusterConfigRequest(username, password));
                }
            })
            .map(new Func1<ClusterConfigResponse, ClusterInfo>() {
                @Override
                public ClusterInfo call(ClusterConfigResponse response) {
                    try {
                        return new DefaultClusterInfo(CouchbaseAsyncBucket.JSON_OBJECT_TRANSCODER.stringToJsonObject(response.config()));
                    } catch (Exception e) {
                        throw new TranscodingException("Could not decode cluster info.", e);
                    }
                }
            });
    }

    @Override
    public Observable<BucketSettings> getBuckets() {
        return
            ensureServiceEnabled()
            .flatMap(new Func1<Boolean, Observable<BucketsConfigResponse>>() {
                @Override
                public Observable<BucketsConfigResponse> call(Boolean aBoolean) {
                    return core.send(new BucketsConfigRequest(username, password));
                }
            }).flatMap(new Func1<BucketsConfigResponse, Observable<BucketSettings>>() {
                @Override
                public Observable<BucketSettings> call(BucketsConfigResponse response) {
                    try {
                        JsonArray decoded = CouchbaseAsyncBucket.JSON_ARRAY_TRANSCODER.stringToJsonArray(response.config());
                        List<BucketSettings> settings = new ArrayList<BucketSettings>();
                        for (Object item : decoded) {
                            JsonObject bucket = (JsonObject) item;
                            settings.add(DefaultBucketSettings.builder()
                                .name(bucket.getString("name"))
                                .enableFlush(bucket.getObject("controllers").getString("flush") != null)
                                .type(bucket.getString("bucketType").equals("membase")
                                    ? BucketType.COUCHBASE : BucketType.MEMCACHED)
                                .replicas(bucket.getInt("replicaNumber"))
                                .quota(bucket.getObject("quota").getInt("ram"))
                                .indexReplicas(bucket.getBoolean("replicaIndex"))
                                .port(bucket.getInt("proxyPort"))
                                .password(bucket.getString("saslPassword"))
                                .build());
                        }
                        return Observable.from(settings);
                    } catch (Exception e) {
                        throw new TranscodingException("Could not decode cluster info.", e);
                    }
                }
            });
    }

    @Override
    public Observable<BucketSettings> getBucket(final String name) {
        return getBuckets().filter(new Func1<BucketSettings, Boolean>() {
            @Override
            public Boolean call(BucketSettings bucketSettings) {
                return bucketSettings.name().equals(name);
            }
        });
    }

    @Override
    public Observable<Boolean> hasBucket(final String name) {
        return getBucket(name)
            .isEmpty()
            .map(new Func1<Boolean, Boolean>() {
                @Override
                public Boolean call(Boolean notFound) {
                    return !notFound;
                }
            });
    }

    @Override
    public Observable<Boolean> removeBucket(final String name) {
        return
            ensureServiceEnabled()
            .flatMap(new Func1<Boolean, Observable<RemoveBucketResponse>>() {
                @Override
                public Observable<RemoveBucketResponse> call(Boolean aBoolean) {
                    return core.send(new RemoveBucketRequest(name, username, password));
                }
            }).map(new Func1<RemoveBucketResponse, Boolean>() {
                @Override
                public Boolean call(RemoveBucketResponse response) {
                    return response.status().isSuccess();
                }
            });
    }

    @Override
    public Observable<BucketSettings> insertBucket(final BucketSettings settings) {
        final StringBuilder sb = new StringBuilder();
        sb.append("name=").append(settings.name());
        sb.append("&ramQuotaMB=").append(settings.quota());
        sb.append("&authType=").append("sasl");
        sb.append("&saslPassword=").append(settings.password());
        sb.append("&replicaNumber=").append(settings.replicas());
        sb.append("&proxyPort=").append(settings.port());
        sb.append("&bucketType=").append(settings.type() == BucketType.COUCHBASE ? "membase" : "memcached");
        sb.append("&flushEnabled=").append(settings.enableFlush() ? "1" : "0");

        return hasBucket(settings.name())
            .doOnNext(new Action1<Boolean>() {
                @Override
                public void call(Boolean exists) {
                    if (exists) {
                        throw new BucketAlreadyExistsException("Bucket " + settings.name() + " already exists!");
                    }
                }
            }).flatMap(new Func1<Boolean, Observable<InsertBucketResponse>>() {
                @Override
                public Observable<InsertBucketResponse> call(Boolean exists) {
                    return core.send(new InsertBucketRequest(sb.toString(), username, password));
                }
            })
            .map(new Func1<InsertBucketResponse, BucketSettings>() {
                @Override
                public BucketSettings call(InsertBucketResponse response) {
                    if (!response.status().isSuccess()) {
                        throw new CouchbaseException("Could not insert bucket: " + response.config());
                    }
                    return settings;
                }
            });
    }

    @Override
    public Observable<BucketSettings> updateBucket(final BucketSettings settings) {
        final StringBuilder sb = new StringBuilder();
        sb.append("ramQuotaMB=").append(settings.quota());
        sb.append("&authType=").append("sasl");
        sb.append("&saslPassword=").append(settings.password());
        sb.append("&replicaNumber=").append(settings.replicas());
        sb.append("&proxyPort=").append(settings.port());
        sb.append("&bucketType=").append(settings.type() == BucketType.COUCHBASE ? "membase" : "memcached");
        sb.append("&flushEnabled=").append(settings.enableFlush() ? "1" : "0");

        return hasBucket(settings.name())
            .doOnNext(new Action1<Boolean>() {
                @Override
                public void call(Boolean exists) {
                    if(!exists) {
                        throw new BucketDoesNotExistException("Bucket " + settings.name() + " does not exist!");
                    }
                }
            }).flatMap(new Func1<Boolean, Observable<UpdateBucketResponse>>() {
                @Override
                public Observable<UpdateBucketResponse> call(Boolean exists) {
                    return core.send(new UpdateBucketRequest(settings.name(), sb.toString(), username, password));
                }
            }).map(new Func1<UpdateBucketResponse, BucketSettings>() {
                @Override
                public BucketSettings call(UpdateBucketResponse response) {
                    if (!response.status().isSuccess()) {
                        throw new CouchbaseException("Could not update bucket: " + response.config());
                    }
                    return settings;
                }
            });
    }

    private Observable<Boolean> ensureServiceEnabled() {
        return Observable
            .just(connectionString.hosts().get(0).getHostName())
            .map(new Func1<String, InetAddress>() {
                @Override
                public InetAddress call(String hostname) {
                    try {
                        return InetAddress.getByName(hostname);
                    } catch(UnknownHostException e) {
                        throw new CouchbaseException(e);
                    }
                }
            })
            .flatMap(new Func1<InetAddress, Observable<AddServiceResponse>>() {
                @Override
                public Observable<AddServiceResponse> call(final InetAddress hostname) {
                    return core
                        .<AddNodeResponse>send(new AddNodeRequest(hostname))
                        .flatMap(new Func1<AddNodeResponse, Observable<AddServiceResponse>>() {
                            @Override
                            public Observable<AddServiceResponse> call(AddNodeResponse response) {
                                int port = environment.sslEnabled()
                                    ? environment.bootstrapHttpSslPort() : environment.bootstrapHttpDirectPort();
                                return core.send(new AddServiceRequest(ServiceType.CONFIG, username, password,
                                    port, hostname));
                            }
                        });
                }
            })
            .map(new Func1<AddServiceResponse, Boolean>() {
                @Override
                public Boolean call(AddServiceResponse addServiceResponse) {
                    if (!addServiceResponse.status().isSuccess()) {
                        throw new CouchbaseException("Could not enable ClusterManager service to function properly.");
                    }
                    return true;
                }
            });
    }

}
