package com.couchbase.client.java;

import rx.Observable;

public interface Cluster {

  Observable<Bucket> openBucket(String name);
  Observable<Bucket> openBucket(String name, String password);
}
