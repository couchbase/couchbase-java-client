package com.couchbase.client.java.bucket;

import com.couchbase.client.java.document.json.JsonObject;

public interface BucketInfo {

    String name();

    BucketType type();

    int nodeCount();

    int replicaCount();

    JsonObject raw();

}
