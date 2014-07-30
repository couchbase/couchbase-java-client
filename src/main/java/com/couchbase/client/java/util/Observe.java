package com.couchbase.client.java.util;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.config.CouchbaseBucketConfig;
import com.couchbase.client.core.message.binary.ObserveRequest;
import com.couchbase.client.core.message.binary.ObserveResponse;
import com.couchbase.client.core.message.cluster.GetClusterConfigRequest;
import com.couchbase.client.core.message.cluster.GetClusterConfigResponse;
import com.couchbase.client.java.PersistTo;
import com.couchbase.client.java.ReplicateTo;
import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility class to handle observe calls and polling logic.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class Observe {

    public static Observable<Boolean> call(final ClusterFacade core, final String bucket, final String id,
        final long cas, final boolean remove, final PersistTo persistTo, final ReplicateTo replicateTo) {

        final ObserveResponse.ObserveStatus persistIdentifier;
        final ObserveResponse.ObserveStatus replicaIdentifier;
        if (remove) {
            persistIdentifier = ObserveResponse.ObserveStatus.FOUND_NOT_PERSISTED;
            replicaIdentifier = ObserveResponse.ObserveStatus.NOT_FOUND_NOT_PERSISTED;
        } else {
            persistIdentifier =  ObserveResponse.ObserveStatus.FOUND_PERSISTED;
            replicaIdentifier = ObserveResponse.ObserveStatus.NOT_FOUND_PERSISTED;
        }

        return sendObserveRequests(core, bucket, id, cas, persistTo)
            .toList()
            .delay(10, TimeUnit.MILLISECONDS)
            .repeat()
            .skipWhile(new Func1<List<ObserveResponse>, Boolean>() {
                @Override
                public Boolean call(List<ObserveResponse> observeResponses) {
                    int replicated = 0;
                    int persisted = 0;
                    boolean persistedMaster = false;
                    for (ObserveResponse response : observeResponses) {
                        ObserveResponse.ObserveStatus status = response.observeStatus();
                        if (response.master()) {
                            if (status == persistIdentifier) {
                                persisted++;
                                persistedMaster = true;
                            }
                        } else {
                            if (status == persistIdentifier) {
                                persisted++;
                                replicated++;
                            } else if (status == replicaIdentifier) {
                                replicated++;
                            }
                        }
                    }

                    boolean persistDone = false;
                    boolean replicateDone = false;

                    if (persistTo == PersistTo.MASTER && persistedMaster) {
                        persistDone = true;
                    } else if (persisted >= persistTo.value()) {
                        persistDone = true;
                    }

                    if (replicated >= replicateTo.value()) {
                        replicateDone = true;
                    }

                    return !(persistDone && replicateDone);
                }
            })
            .take(1)
            .map(new Func1<List<ObserveResponse>, Boolean>() {
                @Override
                public Boolean call(List<ObserveResponse> observeResponses) {
                    return true;
                }
            });
    }

    private static Observable<ObserveResponse> sendObserveRequests(final ClusterFacade core, final String bucket, final String id, final long cas,
        final PersistTo persistTo) {
        return Observable.defer(new Func0<Observable<ObserveResponse>>() {
            @Override
            public Observable<ObserveResponse> call() {
                return core
                    .<GetClusterConfigResponse>send(new GetClusterConfigRequest())
                    .map(new Func1<GetClusterConfigResponse, Integer>() {
                        @Override
                        public Integer call(GetClusterConfigResponse response) {
                            CouchbaseBucketConfig conf = (CouchbaseBucketConfig) response.config().bucketConfig(bucket);
                            return conf.numberOfReplicas();
                        }
                    })
                    .flatMap(new Func1<Integer, Observable<ObserveResponse>>() {
                        @Override
                        public Observable<ObserveResponse> call(Integer replicas) {
                            List<Observable<ObserveResponse>> obs = new ArrayList<Observable<ObserveResponse>>();
                            if (persistTo != PersistTo.NONE) {
                                obs.add(core.<ObserveResponse>send(new ObserveRequest(id, cas, true, (short) 0, bucket)));
                            }
                            if (replicas >= 1) {
                                obs.add(core.<ObserveResponse>send(new ObserveRequest(id, cas, false, (short) 1, bucket)));
                            }
                            if (replicas >= 2) {
                                obs.add(core.<ObserveResponse>send(new ObserveRequest(id, cas, false, (short) 2, bucket)));
                            }
                            if (replicas == 3) {
                                obs.add(core.<ObserveResponse>send(new ObserveRequest(id, cas, false, (short) 3, bucket)));
                            }
                            return Observable.merge(obs);
                        }
                    });
            }
        });
    }
}
