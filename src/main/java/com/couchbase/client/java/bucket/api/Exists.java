/*
 * Copyright (c) 2018 Couchbase, Inc.
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
package com.couchbase.client.java.bucket.api;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.message.kv.ObserveRequest;
import com.couchbase.client.core.message.kv.ObserveResponse;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;

import java.util.concurrent.TimeUnit;

import static com.couchbase.client.java.bucket.api.Utils.addRequestSpan;
import static com.couchbase.client.java.bucket.api.Utils.applyTimeout;
import static com.couchbase.client.java.util.OnSubscribeDeferAndWatch.deferAndWatch;

/**
 * Contains the logic to execute and handle exists requests.
 *
 * @author Michael Nitschinger
 * @since 2.6.0
 */
@InterfaceAudience.Private
@InterfaceStability.Uncommitted
public class Exists {

    public static Observable<Boolean> exists(final String id, final CouchbaseEnvironment environment,
        final ClusterFacade core, final String bucket, final long timeout,
        final TimeUnit timeUnit) {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                final ObserveRequest request = new ObserveRequest(id, 0, true, (short) 0, bucket);
                addRequestSpan(environment, request, "exists");
                return applyTimeout(deferAndWatch(new Func1<Subscriber, Observable<ObserveResponse>>() {
                    @Override
                    public Observable<ObserveResponse> call(Subscriber s) {
                        request.subscriber(s);
                        return core.send(request);
                    }
                }) .map(new Func1<ObserveResponse, Boolean>() {
                    @Override
                    public Boolean call(ObserveResponse response) {
                        ByteBuf content = response.content();
                        if (content != null && content.refCnt() > 0) {
                            content.release();
                        }

                        if (environment.operationTracingEnabled()) {
                            environment.tracer().scopeManager()
                                .activate(response.request().span(), true)
                                .close();
                        }

                        ObserveResponse.ObserveStatus foundStatus = response.observeStatus();
                        return foundStatus == ObserveResponse.ObserveStatus.FOUND_PERSISTED
                            || foundStatus == ObserveResponse.ObserveStatus.FOUND_NOT_PERSISTED;
                    }
                }), request, environment, timeout, timeUnit);
            }
        });
    }
}
