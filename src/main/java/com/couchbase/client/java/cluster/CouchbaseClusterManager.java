package com.couchbase.client.java.cluster;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.java.ConnectionString;
import com.couchbase.client.java.env.CouchbaseEnvironment;

/**
 * .
 *
 * @author Michael Nitschinger
 */
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
    }

    private Observable<Boolean> ensureServiceEnabled() {
        int port = environment.properties().sslEnabled()
            ? environment.properties().bootstrapHttpSslPort() : environment.properties().bootstrapHttpDirectPort();
        InetAddress hostname = connectionString.hosts().get(0).getAddress();
        return core.<AddServiceResponse>send(new AddServiceRequest(ServiceType.CONFIG, username, password, port, hostname))
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
    */
}
