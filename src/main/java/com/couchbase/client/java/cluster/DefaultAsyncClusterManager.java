/*
 * Copyright (c) 2016 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.client.java.cluster;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.message.config.*;
import com.couchbase.client.core.message.internal.AddNodeRequest;
import com.couchbase.client.core.message.internal.AddNodeResponse;
import com.couchbase.client.core.message.internal.AddServiceRequest;
import com.couchbase.client.core.message.internal.AddServiceResponse;
import com.couchbase.client.core.service.ServiceType;
import com.couchbase.client.java.ConnectionString;
import com.couchbase.client.java.CouchbaseAsyncBucket;
import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.cluster.api.AsyncClusterApiClient;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.error.BucketAlreadyExistsException;
import com.couchbase.client.java.error.BucketDoesNotExistException;
import com.couchbase.client.java.error.InvalidPasswordException;
import com.couchbase.client.java.error.TranscodingException;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DefaultAsyncClusterManager implements AsyncClusterManager {

    final ClusterFacade core;
    final String username;
    final String password;
    final CouchbaseEnvironment environment;
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
    @InterfaceStability.Experimental
    public Observable<AsyncClusterApiClient> apiClient() {
        return ensureServiceEnabled()
                .map(new Func1<Boolean, AsyncClusterApiClient>() {
                    @Override
                    public AsyncClusterApiClient call(Boolean aBoolean) {
                        return new AsyncClusterApiClient(username, password, core);
                    }
                });
    }

    @Override
    public Observable<ClusterInfo> info() {
        return ensureServiceEnabled()
            .flatMap(new Func1<Boolean, Observable<ClusterConfigResponse>>() {
                @Override
                public Observable<ClusterConfigResponse> call(Boolean aBoolean) {
                    return core.send(new ClusterConfigRequest(username, password));
                }
            })
            .doOnNext(new Action1<ClusterConfigResponse>() {
                @Override
                public void call(ClusterConfigResponse response) {
                    if (!response.status().isSuccess()) {
                        if (response.config().contains("Unauthorized")) {
                            throw new InvalidPasswordException();
                        } else {
                            throw new CouchbaseException(response.status() + ": " + response.config());
                        }
                    }
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
        return ensureServiceEnabled()
            .flatMap(new Func1<Boolean, Observable<BucketsConfigResponse>>() {
                @Override
                public Observable<BucketsConfigResponse> call(Boolean aBoolean) {
                    return core.send(new BucketsConfigRequest(username, password));
                }
            })
            .doOnNext(new Action1<BucketsConfigResponse>() {
                @Override
                public void call(BucketsConfigResponse response) {
                    if (!response.status().isSuccess()) {
                        if (response.config().contains("Unauthorized")) {
                            throw new InvalidPasswordException();
                        } else {
                            throw new CouchbaseException(response.status() + ": " + response.config());
                        }
                    }
                }
            })
            .flatMap(new Func1<BucketsConfigResponse, Observable<BucketSettings>>() {
                @Override
                public Observable<BucketSettings> call(BucketsConfigResponse response) {
                    try {
                        JsonArray decoded = CouchbaseAsyncBucket.JSON_ARRAY_TRANSCODER.stringToJsonArray(response.config());
                        List<BucketSettings> settings = new ArrayList<BucketSettings>();
                        for (Object item : decoded) {
                            JsonObject bucket = (JsonObject) item;
                            JsonObject controllers = bucket.getObject("controllers");
                            boolean enableFlush = controllers != null && controllers.getString("flush") != null;
                            Boolean replicaIndex = bucket.getBoolean("replicaIndex");
                            boolean indexReplicas = replicaIndex != null ? replicaIndex : false;
                            int ramQuota = 0;
                            if (bucket.getObject("quota").get("ram") instanceof Long) {
                                ramQuota = (int) (bucket.getObject("quota").getLong("ram") / 1024 / 1024);
                            } else {
                                ramQuota = bucket.getObject("quota").getInt("ram") / 1024 / 1024;
                            }
                            BucketType bucketType = "membase".equalsIgnoreCase(bucket.getString("bucketType")) ?
                                BucketType.COUCHBASE : BucketType.MEMCACHED;

                            settings.add(DefaultBucketSettings.builder()
                                    .name(bucket.getString("name"))
                                    .enableFlush(enableFlush)
                                    .type(bucketType)
                                    .replicas(bucket.getInt("replicaNumber"))
                                    .quota(ramQuota)
                                    .indexReplicas(indexReplicas)
                                    .port(bucket.getInt("proxyPort"))
                                    .password(bucket.getString("saslPassword"))
                                    .build(bucket));
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
        final String payload = getConfigureBucketPayload(settings, true);

        return ensureBucketIsHealthy(hasBucket(settings.name())
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
                    return core.send(new InsertBucketRequest(payload, username, password));
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
            }));
    }

    @Override
    public Observable<BucketSettings> updateBucket(final BucketSettings settings) {
        final String payload = getConfigureBucketPayload(settings, false);

        return ensureBucketIsHealthy(hasBucket(settings.name())
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
                    return core.send(new UpdateBucketRequest(settings.name(), payload, username, password));
                }
            }).map(new Func1<UpdateBucketResponse, BucketSettings>() {
                @Override
                public BucketSettings call(UpdateBucketResponse response) {
                    if (!response.status().isSuccess()) {
                        throw new CouchbaseException("Could not update bucket: " + response.config());
                    }
                    return settings;
                }
            }));
    }

    protected String getConfigureBucketPayload(BucketSettings settings, boolean includeName) {
        Map<String, Object> customSettings = settings.customSettings();
        Map<String, Object> actual = new LinkedHashMap<String, Object>(8 + customSettings.size());

        if (includeName) {
            actual.put("name", settings.name());
        }
        actual.put("ramQuotaMB", settings.quota());
        actual.put("authType", "sasl");
        actual.put("saslPassword", settings.password());
        actual.put("replicaNumber", settings.replicas());
        actual.put("proxyPort", settings.port());
        actual.put("bucketType", settings.type() == BucketType.COUCHBASE ? "membase" : "memcached");
        actual.put("flushEnabled", settings.enableFlush() ? "1" : "0");
        for (Map.Entry<String, Object> customSetting : customSettings.entrySet()) {
            if (actual.containsKey(customSetting.getKey()) || (!includeName && "name".equals(customSetting.getKey()))) {
                continue;
            }
            actual.put(customSetting.getKey(), customSetting.getValue());
        }

        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> setting : actual.entrySet()) {
            sb.append('&').append(setting.getKey()).append('=').append(setting.getValue());
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(0);
        }
        return sb.toString();
    }

    /**
     * Helper method to ensure that the state of a bucket on all nodes is healthy.
     *
     * This polling logic is in place as a workaround because the bucket could still be warming up or completing
     * its creation process before any actual operation can be performed.
     *
     * @return the original input stream once done.
     */
    private Observable<BucketSettings> ensureBucketIsHealthy(final Observable<BucketSettings> input) {
        return input.flatMap(new Func1<BucketSettings, Observable<BucketSettings>>() {
            @Override
            public Observable<BucketSettings> call(final BucketSettings bucketSettings) {
                return info()
                    .delay(100, TimeUnit.MILLISECONDS)
                    .filter(new Func1<ClusterInfo, Boolean>() {
                        @Override
                        public Boolean call(ClusterInfo clusterInfo) {
                            boolean allHealthy = true;
                            for (Object n : clusterInfo.raw().getArray("nodes")) {
                                JsonObject node = (JsonObject) n;
                                if (!node.getString("status").equals("healthy")) {
                                    allHealthy = false;
                                    break;
                                }
                            }
                            return allHealthy;
                        }
                    })
                    .repeat()
                    .take(1)
                    .flatMap(new Func1<ClusterInfo, Observable<BucketSettings>>() {
                        @Override
                        public Observable<BucketSettings> call(ClusterInfo clusterInfo) {
                            return Observable.just(bucketSettings);
                        }
                    });
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