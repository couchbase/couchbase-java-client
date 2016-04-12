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
package com.couchbase.client.java.bucket;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.config.CouchbaseBucketConfig;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.message.cluster.GetClusterConfigRequest;
import com.couchbase.client.core.message.cluster.GetClusterConfigResponse;
import com.couchbase.client.core.message.kv.BinaryRequest;
import com.couchbase.client.core.message.kv.GetRequest;
import com.couchbase.client.core.message.kv.GetResponse;
import com.couchbase.client.core.message.kv.ReplicaGetRequest;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.ReplicaMode;
import com.couchbase.client.java.error.CouchbaseOutOfMemoryException;
import com.couchbase.client.java.error.TemporaryFailureException;
import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to deal with reading from zero to N replicas and returning results.
 *
 * @author Michael Nitschinger
 * @since 2.1.4
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Private
public class ReplicaReader {

    /**
     * The logger used.
     */
    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(ReplicaReader.class);

    /**
     * Perform replica reads to as many nodes a possible based on the given {@link ReplicaMode}.
     *
     * Individual errors are swallowed, but logged.
     *
     * @param core the core reference.
     * @param id the id of the document to load from the replicas.
     * @param type the replica mode type.
     * @param bucket the name of the bucket to load it from.
     * @return a potentially empty observable with the returned raw responses.
     */
    public static Observable<GetResponse> read(final ClusterFacade core, final String id,
        final ReplicaMode type, final String bucket) {
        return assembleRequests(core, id, type, bucket)
            .flatMap(new Func1<BinaryRequest, Observable<GetResponse>>() {
                @Override
                public Observable<GetResponse> call(BinaryRequest request) {
                    return core
                        .<GetResponse>send(request)
                        .filter(GetResponseFilter.INSTANCE)
                        .onErrorResumeNext(GetResponseErrorHandler.INSTANCE);
                }
            });
    }

    /**
     * Helper method to assemble all possible/needed replica get requests.
     *
     * The number of configured replicas is also loaded on demand for each request. In the future, this can be
     * maybe optimized.
     *
     * @param core the core reference.
     * @param id the id of the document to load from the replicas.
     * @param type the replica mode type.
     * @param bucket the name of the bucket to load it from.
     * @return a list of requests to perform (both regular and replica get).
     */
    private static Observable<BinaryRequest> assembleRequests(final ClusterFacade core, final String id,
        final ReplicaMode type, final String bucket) {
        if (type != ReplicaMode.ALL) {
            return Observable.just((BinaryRequest) new ReplicaGetRequest(id, bucket, (short) type.ordinal()));
        }

        return Observable.defer(new Func0<Observable<GetClusterConfigResponse>>() {
                @Override
                public Observable<GetClusterConfigResponse> call() {
                    return core.send(new GetClusterConfigRequest());
                }
            })
            .map(new Func1<GetClusterConfigResponse, Integer>() {
                @Override
                public Integer call(GetClusterConfigResponse response) {
                    CouchbaseBucketConfig conf = (CouchbaseBucketConfig) response.config().bucketConfig(bucket);
                    return conf.numberOfReplicas();
                }
            })
            .flatMap(new Func1<Integer, Observable<BinaryRequest>>() {
                @Override
                public Observable<BinaryRequest> call(Integer max) {
                    List<BinaryRequest> requests = new ArrayList<BinaryRequest>();

                    requests.add(new GetRequest(id, bucket));
                    for (int i = 0; i < max; i++) {
                        requests.add(new ReplicaGetRequest(id, bucket, (short) (i + 1)));
                    }
                    return Observable.from(requests);
                }
            });
    }

    /**
     * A filter for the responses.
     *
     * This filter checks the response status and releases resources as needed.
     */
    private static class GetResponseFilter implements Func1<GetResponse, Boolean> {

        public static GetResponseFilter INSTANCE = new GetResponseFilter();

        @Override
        public Boolean call(GetResponse response) {
            if (response.status().isSuccess()) {
                return true;
            }
            ByteBuf content = response.content();
            if (content != null && content.refCnt() > 0) {
                content.release();
            }

            switch (response.status()) {
                case NOT_EXISTS:
                    return false;
                case TEMPORARY_FAILURE:
                case SERVER_BUSY:
                    throw new TemporaryFailureException();
                case OUT_OF_MEMORY:
                    throw new CouchbaseOutOfMemoryException();
                default:
                    throw new CouchbaseException(response.status().toString());
            }
        }
    }

    /**
     * This error handler silences all errors, but also logs them properly.
     */
    private static class GetResponseErrorHandler implements Func1<Throwable, Observable<? extends GetResponse>> {

        public static GetResponseErrorHandler INSTANCE = new GetResponseErrorHandler();

        @Override
        public Observable<? extends GetResponse> call(Throwable throwable) {
            LOGGER.info("Individual ReplicaGet failed, but ignoring. Reason: {}", throwable.toString());
            return Observable.empty();
        }
    }

}
