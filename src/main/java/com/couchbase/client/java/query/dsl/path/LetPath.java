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
