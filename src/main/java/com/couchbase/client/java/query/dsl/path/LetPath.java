package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Alias;
import com.couchbase.client.java.query.dsl.Expression;

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

    JoinPath join(Expression from);

    JoinPath innerJoin(Expression from);

    JoinPath leftJoin(Expression from);

    JoinPath leftOuterJoin(Expression from);

    NestPath nest(Expression from);

    NestPath innerNest(Expression from);

    NestPath leftNest(Expression from);

    NestPath leftOuterNest(Expression from);

    UnnestPath unnest(Expression path);

    UnnestPath innerUnnest(Expression path);

    UnnestPath leftUnnest(Expression path);

    UnnestPath leftOuterUnnest(Expression path);

}
