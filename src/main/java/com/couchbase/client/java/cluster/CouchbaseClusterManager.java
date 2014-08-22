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
import com.couchbase.client.java.CouchbaseBucket;
import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class CouchbaseClusterManager implements ClusterManager {

    private final ClusterFacade core;
    private final String username;
    private final String password;
    private final CouchbaseEnvironment environment;
    private final ConnectionString connectionString;

    CouchbaseClusterManager(final String username, final String password, final ConnectionString connectionString,
        final CouchbaseEnvironment environment, final ClusterFacade core) {
        this.username = username;
        this.password = password;
        this.core = core;
        this.environment = environment;
        this.connectionString = connectionString;
    }

    public static CouchbaseClusterManager create(final String username, final String password,
        final ConnectionString connectionString, final CouchbaseEnvironment environment, final ClusterFacade core) {
        return new CouchbaseClusterManager(username, password, connectionString, environment, core);
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
                        return new DefaultClusterInfo(CouchbaseBucket.JSON_TRANSCODER.stringToJsonObject(response.config()));
                    } catch (Exception e) {
                        throw new CouchbaseException("Could not decode cluster info.", e);
                    }
                }
            });
    }

    @Override
    public Observable<ClusterBucketSettings> getBuckets() {
        return
            ensureServiceEnabled()
            .flatMap(new Func1<Boolean, Observable<BucketsConfigResponse>>() {
                @Override
                public Observable<BucketsConfigResponse> call(Boolean aBoolean) {
                    return core.send(new BucketsConfigRequest(username, password));
                }
            }).flatMap(new Func1<BucketsConfigResponse, Observable<ClusterBucketSettings>>() {
                @Override
                public Observable<ClusterBucketSettings> call(BucketsConfigResponse response) {
                    try {
                        JsonArray decoded = CouchbaseBucket.JSON_TRANSCODER.stringTojsonArray(response.config());
                        List<ClusterBucketSettings> settings = new ArrayList<ClusterBucketSettings>();
                        for (Object item : decoded) {
                            JsonObject bucket = (JsonObject) item;
                            settings.add(DefaultClusterBucketSettings.builder()
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
                        throw new CouchbaseException("Could not decode cluster info.", e);
                    }
                }
            });
    }

    @Override
    public Observable<ClusterBucketSettings> getBucket(final String name) {
        return getBuckets().filter(new Func1<ClusterBucketSettings, Boolean>() {
            @Override
            public Boolean call(ClusterBucketSettings bucketSettings) {
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
    public Observable<ClusterBucketSettings> insertBucket(final ClusterBucketSettings settings) {
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
                        throw new BucketExistsException("Bucket " + settings.name() + " already exists!");
                    }
                }
            }).flatMap(new Func1<Boolean, Observable<InsertBucketResponse>>() {
                @Override
                public Observable<InsertBucketResponse> call(Boolean exists) {
                    return core.send(new InsertBucketRequest(sb.toString(), username, password));
                }
            })
            .map(new Func1<InsertBucketResponse, ClusterBucketSettings>() {
                @Override
                public ClusterBucketSettings call(InsertBucketResponse response) {
                    if (!response.status().isSuccess()) {
                        throw new CouchbaseException("Could not insert bucket: " + response.config());
                    }
                    return settings;
                }
            });
    }

    @Override
    public Observable<ClusterBucketSettings> updateBucket(final ClusterBucketSettings settings) {
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
            }).map(new Func1<UpdateBucketResponse, ClusterBucketSettings>() {
                @Override
                public ClusterBucketSettings call(UpdateBucketResponse response) {
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
