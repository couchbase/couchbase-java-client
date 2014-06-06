package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Alias;

public interface LetPath extends WherePath {

    WherePath let(Alias... aliases);

    JoinPath join(String from);

    JoinPath innerJoin(String from);

    JoinPath leftJoin(String from);

    JoinPath leftOuterJoin(String from);

    NestPath nest(String from);

    NestPath innerNest(String from);

    NestPath leftNest(String from);

    NestPath leftOuterNest(String from);

    UnnestPath unnest(String path);

    UnnestPath innerUnnest(String path);

    UnnestPath leftUnnest(String path);

    UnnestPath leftOuterUnnest(String path);

}
