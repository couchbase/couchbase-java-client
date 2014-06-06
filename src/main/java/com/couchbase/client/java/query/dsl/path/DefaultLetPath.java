package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Alias;
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
}
