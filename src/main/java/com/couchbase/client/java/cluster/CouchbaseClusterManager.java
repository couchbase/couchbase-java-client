package com.couchbase.client.java.cluster;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.message.config.ClusterConfigRequest;
import com.couchbase.client.core.message.config.ClusterConfigResponse;
import com.couchbase.client.core.message.internal.AddNodeRequest;
import com.couchbase.client.core.message.internal.AddNodeResponse;
import com.couchbase.client.core.message.internal.AddServiceRequest;
import com.couchbase.client.core.message.internal.AddServiceResponse;
import com.couchbase.client.core.service.ServiceType;
import com.couchbase.client.java.ConnectionString;
import com.couchbase.client.java.CouchbaseBucket;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import rx.Observable;
import rx.functions.Func1;

import java.net.InetAddress;
import java.net.UnknownHostException;

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

    /*
    @Override
    public Observable<ClusterBucketSettings> getBuckets() {
        /*return ensureServiceEnabled()
            .flatMap(new Func1<Boolean, Observable<GetBucketsResponse>>() {
                @Override
                public Observable<GetBucketsResponse> call(Boolean serviceEnabled) {
                    return core.send(new GetBucketsRequest());
                }
            }).map(new Func1<GetBucketsResponse, ClusterBucketSettings>() {
                @Override
                public ClusterBucketSettings call(GetBucketsResponse getBucketsResponse) {
                    return null;
                }
            });*/
        /*return null;
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
    public Observable<ClusterBucketSettings> removeBucket(String name) {
        return null;
    }

    @Override
    public Observable<ClusterBucketSettings> insertBucket(ClusterBucketSettings bucketSettings) {
        return null;
    }

    @Override
    public Observable<ClusterBucketSettings> updateBucket(ClusterBucketSettings bucketSettings) {
        return null;
    }*/

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
