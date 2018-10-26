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
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.message.config.BucketsConfigRequest;
import com.couchbase.client.core.message.config.BucketsConfigResponse;
import com.couchbase.client.core.message.config.ClusterConfigRequest;
import com.couchbase.client.core.message.config.ClusterConfigResponse;
import com.couchbase.client.core.message.config.GetUsersRequest;
import com.couchbase.client.core.message.config.GetUsersResponse;
import com.couchbase.client.core.message.config.InsertBucketRequest;
import com.couchbase.client.core.message.config.InsertBucketResponse;
import com.couchbase.client.core.message.config.RemoveBucketRequest;
import com.couchbase.client.core.message.config.RemoveBucketResponse;
import com.couchbase.client.core.message.config.RemoveUserRequest;
import com.couchbase.client.core.message.config.RemoveUserResponse;
import com.couchbase.client.core.message.config.UpdateBucketRequest;
import com.couchbase.client.core.message.config.UpdateBucketResponse;
import com.couchbase.client.core.message.config.UpsertUserRequest;
import com.couchbase.client.core.message.config.UpsertUserResponse;
import com.couchbase.client.core.message.internal.AddNodeRequest;
import com.couchbase.client.core.message.internal.AddNodeResponse;
import com.couchbase.client.core.message.internal.AddServiceRequest;
import com.couchbase.client.core.message.internal.AddServiceResponse;
import com.couchbase.client.core.service.ServiceType;
import com.couchbase.client.core.time.Delay;
import com.couchbase.client.core.utils.ConnectionString;
import com.couchbase.client.core.utils.NetworkAddress;
import com.couchbase.client.java.CouchbaseAsyncBucket;
import com.couchbase.client.java.CouchbaseAsyncCluster;
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
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.couchbase.client.java.util.OnSubscribeDeferAndWatch.deferAndWatch;
import static com.couchbase.client.java.util.retry.RetryBuilder.any;

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
                    return deferAndWatch(new Func1<Subscriber, Observable<? extends ClusterConfigResponse>>() {
                        @Override
                        public Observable<? extends ClusterConfigResponse> call(Subscriber subscriber) {
                            ClusterConfigRequest request = new ClusterConfigRequest(username, password);
                            request.subscriber(subscriber);
                            return core.send(request);
                        }
                    });
                }
            })
            .retryWhen(any().delay(Delay.fixed(100, TimeUnit.MILLISECONDS)).max(Integer.MAX_VALUE).build())
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
                    return deferAndWatch(new Func1<Subscriber, Observable<? extends BucketsConfigResponse>>() {
                        @Override
                        public Observable<? extends BucketsConfigResponse> call(Subscriber subscriber) {
                            BucketsConfigRequest request = new BucketsConfigRequest(username, password);
                            request.subscriber(subscriber);
                            return core.send(request);
                        }
                    });
                }
            })
            .retryWhen(any().delay(Delay.fixed(100, TimeUnit.MILLISECONDS)).max(Integer.MAX_VALUE).build())
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

                            BucketType bucketType;
                            String rawType = bucket.getString("bucketType");
                            if ("membase".equalsIgnoreCase(rawType)) {
                                bucketType = BucketType.COUCHBASE;
                            } else if ("ephemeral".equalsIgnoreCase(rawType)) {
                                bucketType = BucketType.EPHEMERAL;
                            } else {
                                bucketType = BucketType.MEMCACHED;
                            }

                            CompressionMode compressionMode = null;
                            String rawCompressionMode = bucket.getString("compressionMode");
                            if (rawCompressionMode != null && !rawCompressionMode.isEmpty()) {
                                if ("off".equalsIgnoreCase(rawCompressionMode)) {
                                    compressionMode = CompressionMode.OFF;
                                } else if ("active".equalsIgnoreCase(rawCompressionMode)) {
                                    compressionMode = CompressionMode.ACTIVE;
                                } else {
                                    // unconditional check because this is the default
                                    // on the server
                                    compressionMode = CompressionMode.PASSIVE;
                                }
                            }

                            EjectionMethod ejectionMethod = EjectionMethod.VALUE;
                            String rawEjectionMethod = bucket.getString("evictionPolicy");
                            if (rawEjectionMethod != null && !rawEjectionMethod.isEmpty()) {
                                if ("fullEviction".equalsIgnoreCase(rawEjectionMethod)) {
                                    ejectionMethod = EjectionMethod.FULL;
                                }
                            }

                            settings.add(DefaultBucketSettings.builder()
                                    .name(bucket.getString("name"))
                                    .enableFlush(enableFlush)
                                    .type(bucketType)
                                    .replicas(bucket.getInt("replicaNumber"))
                                    .quota(ramQuota)
                                    .indexReplicas(indexReplicas)
                                    .port(bucket.getInt("proxyPort"))
                                    .password(bucket.getString("saslPassword"))
                                    .compressionMode(compressionMode)
                                    .ejectionMethod(ejectionMethod)
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
                        return deferAndWatch(new Func1<Subscriber, Observable<? extends RemoveBucketResponse>>() {
                            @Override
                            public Observable<? extends RemoveBucketResponse> call(Subscriber subscriber) {
                                RemoveBucketRequest request = new RemoveBucketRequest(name, username, password);
                                request.subscriber(subscriber);
                                return core.send(request);
                            }
                        });
                    }
                })
                .retryWhen(any().delay(Delay.fixed(100, TimeUnit.MILLISECONDS)).max(Integer.MAX_VALUE).build())
                .map(new Func1<RemoveBucketResponse, Boolean>() {
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
                    return deferAndWatch(new Func1<Subscriber, Observable<? extends InsertBucketResponse>>() {
                        @Override
                        public Observable<? extends InsertBucketResponse> call(Subscriber subscriber) {
                            InsertBucketRequest request = new InsertBucketRequest(payload, username, password);
                            request.subscriber(subscriber);
                            return core.send(request);
                        }
                    });
                }
            })
            .retryWhen(any().delay(Delay.fixed(100, TimeUnit.MILLISECONDS)).max(Integer.MAX_VALUE).build())
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
                    return deferAndWatch(new Func1<Subscriber, Observable<? extends UpdateBucketResponse>>() {
                        @Override
                        public Observable<? extends UpdateBucketResponse> call(Subscriber subscriber) {
                            UpdateBucketRequest request = new UpdateBucketRequest(
                                settings.name(), payload, username, password);
                            request.subscriber(subscriber);
                            return core.send(request);
                        }
                    });
                }
            })
            .retryWhen(any().delay(Delay.fixed(100, TimeUnit.MILLISECONDS)).max(Integer.MAX_VALUE).build())
            .map(new Func1<UpdateBucketResponse, BucketSettings>() {
                @Override
                public BucketSettings call(UpdateBucketResponse response) {
                    if (!response.status().isSuccess()) {
                        throw new CouchbaseException("Could not update bucket: " + response.config());
                    }
                    return settings;
                }
            }));
    }

    @Override
    public Observable<Boolean> upsertUser(final AuthDomain domain, final String userid, final UserSettings userSettings) {
        final String payload = getUserSettingsPayload(userSettings);
        return ensureServiceEnabled()
            .flatMap(new Func1<Boolean, Observable<UpsertUserResponse>>() {
                @Override
                public Observable<UpsertUserResponse> call(Boolean aBoolean) {
                    return deferAndWatch(new Func1<Subscriber, Observable<? extends UpsertUserResponse>>() {
                        @Override
                        public Observable<? extends UpsertUserResponse> call(Subscriber subscriber) {
                            UpsertUserRequest request = new UpsertUserRequest(
                                username, password, domain.alias(), userid, payload);
                            request.subscriber(subscriber);
                            return core.send(request);
                        }
                    });
                }
            })
            .retryWhen(any().delay(Delay.fixed(100, TimeUnit.MILLISECONDS)).max(Integer.MAX_VALUE).build())
            .map(new Func1<UpsertUserResponse, Boolean>() {
                @Override
                public Boolean call(UpsertUserResponse response) {
                    if (!response.status().isSuccess()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Could not update user: ");
                        sb.append(response.status());
                        if (response.message().length() > 0) {
                            sb.append(", ");
                            sb.append("msg: ");
                            sb.append(response.message());
                        }
                        throw new CouchbaseException(sb.toString());
                    }
                    return true;
                }
            });
    }

    @Override
    public Observable<Boolean> removeUser(final AuthDomain domain, final String userid) {
        return ensureServiceEnabled()
            .flatMap(new Func1<Boolean, Observable<RemoveUserResponse>>() {
                @Override
                public Observable<RemoveUserResponse> call(Boolean aBoolean) {
                    return deferAndWatch(new Func1<Subscriber, Observable<? extends RemoveUserResponse>>() {
                        @Override
                        public Observable<? extends RemoveUserResponse> call(Subscriber subscriber) {
                            RemoveUserRequest request = new RemoveUserRequest(
                                username, password, domain.alias(), userid);
                            request.subscriber(subscriber);
                            return core.send(request);
                        }
                    });
                }
            })
            .retryWhen(any().delay(Delay.fixed(100, TimeUnit.MILLISECONDS)).max(Integer.MAX_VALUE).build())
            .map(new Func1<RemoveUserResponse, Boolean>() {
                @Override
                public Boolean call(RemoveUserResponse response) {
                    return response.status().isSuccess();
                }
            });
    }

    @Override
    public Observable<User> getUsers(final AuthDomain domain) {
        return getUser(domain,null);
    }

    @Override
    public Observable<User> getUser(final AuthDomain domain, final String userid) {
        return ensureServiceEnabled()
                .flatMap(new Func1<Boolean, Observable<GetUsersResponse>>() {
                    @Override
                    public Observable<GetUsersResponse> call(Boolean aBoolean) {
                        final GetUsersRequest request = (userid == null || userid.isEmpty())
                            ? GetUsersRequest.usersFromDomain(username, password, domain.alias())
                            : GetUsersRequest.user(username, password, domain.alias(), userid);
                        return deferAndWatch(new Func1<Subscriber, Observable<? extends GetUsersResponse>>() {
                            @Override
                            public Observable<? extends GetUsersResponse> call(Subscriber subscriber) {
                                request.subscriber(subscriber);
                                return core.send(request);
                            }
                        });
                    }
                })
                .retryWhen(any().delay(Delay.fixed(100, TimeUnit.MILLISECONDS)).max(Integer.MAX_VALUE).build())
                .doOnNext(new Action1<GetUsersResponse>() {
                    @Override
                    public void call(GetUsersResponse response) {
                        if (!response.status().isSuccess()) {
                            if (response.content().contains("Unauthorized")) {
                                throw new InvalidPasswordException();
                            } else {
                                throw new CouchbaseException(response.status() + ": " + response.content());
                            }
                        }
                    }
                })
                .flatMap(new Func1<GetUsersResponse, Observable<User>>() {
                    @Override
                    public Observable<User> call(GetUsersResponse response) {
                        try {
                            if (userid != null && !userid.isEmpty()) {
                                JsonObject decoded = CouchbaseAsyncBucket.JSON_OBJECT_TRANSCODER.stringToJsonObject(response.content());
                                JsonArray rolesJsonArr = decoded.getArray("roles");
                                UserRole[] userRoles = new UserRole[rolesJsonArr.size()];
                                int i = 0;
                                for (Object role : rolesJsonArr) {
                                    userRoles[i] = new UserRole(((JsonObject) role).getString("role"), ((JsonObject) role).getString("bucket_name"));
                                    i++;
                                }
                                User user = new User(decoded.getString("name"), decoded.getString("id"),
                                        AuthDomain.fromAlias(decoded.getString("domain")), userRoles);
                                return Observable.just(user);
                            } else {
                                JsonArray decoded = CouchbaseAsyncBucket.JSON_ARRAY_TRANSCODER.stringToJsonArray(response.content());
                                List<User> users = new ArrayList<User>();
                                for (Object item : decoded) {
                                    JsonObject userJsonObj = (JsonObject) item;
                                    JsonArray rolesJsonArr = userJsonObj.getArray("roles");
                                    UserRole[] userRoles = new UserRole[rolesJsonArr.size()];
                                    int i = 0;
                                    for (Object role : rolesJsonArr) {
                                        userRoles[i] = new UserRole(((JsonObject) role).getString("role"), ((JsonObject) role).getString("bucket_name"));
                                        i++;
                                    }
                                    User user = new User(userJsonObj.getString("name"), userJsonObj.getString("id"),
                                            AuthDomain.fromAlias(userJsonObj.getString("domain")), userRoles);
                                    users.add(user);
                                }
                                return Observable.from(users);
                            }
                        } catch (Exception e) {
                            throw new TranscodingException("Could not decode user info.", e);
                        }
                    }
                });
    }

    protected String getConfigureBucketPayload(BucketSettings settings, boolean includeName) {
        Map<String, Object> customSettings = settings.customSettings();
        Map<String, Object> actual = new LinkedHashMap<String, Object>(8 + customSettings.size());

        if (includeName) {
            actual.put("name", settings.name());
        }
        actual.put("ramQuotaMB", settings.quota());
        actual.put("authType", "sasl");
        if (settings.password() != null && !settings.password().isEmpty()) {
            actual.put("saslPassword", settings.password());
        }
        actual.put("replicaNumber", settings.replicas());
        if (settings.port() > 0) {
            actual.put("proxyPort", settings.port());
        }

        if (settings.compressionMode() != null) {
            String compressionMode;
            switch (settings.compressionMode()) {
                case OFF: compressionMode = "off"; break;
                case ACTIVE: compressionMode = "active"; break;
                case PASSIVE: compressionMode = "passive"; break;
                default:
                    throw new UnsupportedOperationException("Could not convert compression mode "
                            + settings.compressionMode());
            }
            actual.put("compressionMode", compressionMode);
        }

        if (settings.ejectionMethod() != null) {
            if (settings.ejectionMethod() == EjectionMethod.FULL) {
                actual.put("evictionPolicy", "fullEviction");
            }
        }

        String bucketType;
        switch(settings.type()) {
            case COUCHBASE: bucketType = "membase"; break;
            case MEMCACHED: bucketType = "memcached"; break;
            case EPHEMERAL: bucketType = "ephemeral"; break;
            default:
                throw new UnsupportedOperationException("Could not convert bucket type " + settings.type());
        }
        actual.put("bucketType", bucketType);
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

    protected String getUserSettingsPayload(UserSettings settings) {
        Map<String, Object> settingsMap = new LinkedHashMap<String, Object>();

        if (settings.name() != null) {
            settingsMap.put("name", settings.name());
        }

        if (settings.password() != null) {
            settingsMap.put("password", settings.password());
        }

        if (settings.roles() != null && settings.roles().size() > 0) {
            StringBuilder sb = new StringBuilder();
            for(UserRole userRole: settings.roles()) {
                if (sb.length() != 0) {
                    sb.append(",");
                }
                sb.append(userRole.role());
                if (userRole.bucket() != null && !userRole.bucket().equals("")) {
                    sb.append("[");
                    sb.append(userRole.bucket().replace("%", "%25"));
                    sb.append("]");
                }
            }
            settingsMap.put("roles", sb.toString());
        }

        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> setting : settingsMap.entrySet()) {
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

    Observable<Boolean> sendAddNodeRequest(final InetSocketAddress address) {
        final NetworkAddress networkAddress = NetworkAddress.create(CouchbaseAsyncCluster.ALLOW_HOSTNAMES_AS_SEED_NODES ?
                address.getHostName() :
                address.getAddress().getHostAddress());
        return core.<AddNodeResponse>send(new AddNodeRequest(networkAddress))
                .flatMap(new Func1<AddNodeResponse, Observable<AddServiceResponse>>() {
                    @Override
                    public Observable<AddServiceResponse> call(AddNodeResponse addNodeResponse) {
                        if (!addNodeResponse.status().isSuccess()) {
                            throw new CouchbaseException("Could not enable ClusterManager service to function properly.");
                        }
                        int port = environment.sslEnabled() ? environment.bootstrapHttpSslPort() : environment.bootstrapHttpDirectPort();
                        return core.send(new AddServiceRequest(ServiceType.CONFIG, username, password, port, networkAddress));
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

    private Observable<Boolean> ensureServiceEnabled() {
        if (connectionString.hosts().isEmpty()) {
            return Observable.error(new IllegalStateException("No host found in the connection string! " + connectionString.toString()));
        }

        final AtomicInteger integer = new AtomicInteger(0);
        return Observable.just(connectionString.hosts())
                .flatMap(new Func1<List<InetSocketAddress>, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(List<InetSocketAddress> inetSocketAddresses) {
                        int hostIndex = integer.getAndIncrement();
                        if (hostIndex >= connectionString.hosts().size()) {
                            integer.set(0);
                            return Observable.error(new CouchbaseException("Could not enable ClusterManager service to function properly."));
                        }
                        return sendAddNodeRequest(inetSocketAddresses.get(hostIndex));
                    }
                });

    }
}