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
import com.couchbase.client.java.query.dsl.element.JoinElement;
import com.couchbase.client.java.query.dsl.element.LetElement;
import com.couchbase.client.java.query.dsl.element.NestElement;
import com.couchbase.client.java.query.dsl.element.UnnestElement;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DefaultLetPath extends DefaultWherePath implements LetPath {

    public DefaultLetPath(AbstractPath parent) {
        super(parent);
    }

    @Override
    public WherePath let(Alias... aliases) {
        element(new LetElement(aliases));
        return new DefaultWherePath(this);
    }

    @Override
    public JoinPath join(String path) {
        element(new JoinElement(JoinType.DEFAULT, path));
        return new DefaultJoinPath(this);
    }

    @Override
    public JoinPath innerJoin(String path) {
        element(new JoinElement(JoinType.INNER, path));
        return new DefaultJoinPath(this);    }

    @Override
    public JoinPath leftJoin(String path) {
        element(new JoinElement(JoinType.LEFT, path));
        return new DefaultJoinPath(this);    }

    @Override
    public JoinPath leftOuterJoin(String path) {
        element(new JoinElement(JoinType.LEFT_OUTER, path));
        return new DefaultJoinPath(this);    }

    @Override
    public NestPath nest(String from) {
        element(new NestElement(JoinType.DEFAULT, from));
        return new DefaultNestPath(this);
    }

    @Override
    public NestPath innerNest(String from) {
        element(new NestElement(JoinType.INNER, from));
        return new DefaultNestPath(this);
    }

    @Override
    public NestPath leftNest(String from) {
        element(new NestElement(JoinType.LEFT, from));
        return new DefaultNestPath(this);
    }

    @Override
    public NestPath leftOuterNest(String from) {
        element(new NestElement(JoinType.LEFT_OUTER, from));
        return new DefaultNestPath(this);
    }

    @Override
    public UnnestPath unnest(String path) {
        element(new UnnestElement(JoinType.DEFAULT, path));
        return new DefaultUnnestPath(this);
    }

    @Override
    public UnnestPath innerUnnest(String path) {
        element(new UnnestElement(JoinType.INNER, path));
        return new DefaultUnnestPath(this);
    }

    @Override
    public UnnestPath leftUnnest(String path) {
        element(new UnnestElement(JoinType.LEFT, path));
        return new DefaultUnnestPath(this);
    }

    @Override
    public UnnestPath leftOuterUnnest(String path) {
        element(new UnnestElement(JoinType.LEFT_OUTER, path));
        return new DefaultUnnestPath(this);
    }

    //===Expression Overrides===

    @Override
    public JoinPath join(Expression from) {
        return join(from.toString());
    }

    @Override
    public JoinPath innerJoin(Expression from) {
        return innerJoin(from.toString());
    }

    @Override
    public JoinPath leftJoin(Expression from) {
        return leftJoin(from.toString());
    }

    @Override
    public JoinPath leftOuterJoin(Expression from) {
        return leftOuterJoin(from.toString());
    }

    @Override
    public NestPath nest(Expression from) {
        return nest(from.toString());
    }

    @Override
    public NestPath innerNest(Expression from) {
        return innerNest(from.toString());
    }

    @Override
    public NestPath leftNest(Expression from) {
        return leftNest(from.toString());
    }

    @Override
    public NestPath leftOuterNest(Expression from) {
        return leftOuterNest(from.toString());
    }

    @Override
    public UnnestPath unnest(Expression path) {
        return unnest(path.toString());
    }

    @Override
    public UnnestPath innerUnnest(Expression path) {
        return innerUnnest(path.toString());
    }

    @Override
    public UnnestPath leftUnnest(Expression path) {
        return leftUnnest(path.toString());
    }

    @Override
    public UnnestPath leftOuterUnnest(Expression path) {
        return leftOuterUnnest(path.toString());
    }
}
