package com.couchbase.client.java.cluster;

import com.couchbase.client.java.document.json.JsonObject;

public class DefaultClusterInfo implements ClusterInfo {

    private final JsonObject raw;

    public DefaultClusterInfo(JsonObject raw) {
        this.raw = raw;
    }

    @Override
    public JsonObject raw() {
        return raw;
    }

}
