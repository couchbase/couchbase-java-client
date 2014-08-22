package com.couchbase.client.java.bucket;

import com.couchbase.client.java.document.json.JsonObject;

public class DefaultBucketInfo implements BucketInfo {

    private final JsonObject raw;

    DefaultBucketInfo(JsonObject raw) {
        this.raw = raw;
    }

    public static DefaultBucketInfo create(JsonObject raw) {
        return new DefaultBucketInfo(raw);
    }

    @Override
    public String name() {
        return raw.getString("name");
    }

    @Override
    public BucketType type() {
        String type = raw.getString("bucketType");
        if (type.equals("membase")) {
            return BucketType.COUCHBASE;
        } else {
            return BucketType.MEMCACHED;
        }
    }

    @Override
    public int nodeCount() {
        return raw.getArray("nodes").size();
    }

    @Override
    public int replicaCount() {
        return raw.getInt("replicaNumber");
    }

    @Override
    public JsonObject raw() {
        return raw;
    }

    @Override
    public String toString() {
        return "DefaultBucketInfo{" +
            "raw=" + raw +
            '}';
    }
}
