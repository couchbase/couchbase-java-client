package com.couchbase.client.java.query.dsl.path;

public interface LimitPath extends OffsetPath {

    OffsetPath limit(int limit);

}
