package com.couchbase.client.java.query;

public class Query {

    private int limit = -1;
    private int offset = -1;
    private Sort[] sorts;
    private Expression groupBy;
    private Expression having;
    private String from;
    private String[] selects;
    private Expression where;

    private Query(String... selects) {
        this.selects = selects;
    }

    public static Query select(String... selects) {
        if (selects.length == 0) {
            throw new IllegalArgumentException("At least one select expression needs to be specified.");
        }
        return new Query(selects);
    }

    public Query where(Expression expression) {
        this.where = expression;
        return this;
    }

    /**
     * Set the datasource/bucket name.
     *
     * @param from
     * @return
     */
    public Query from(final String from) {
        this.from = from;
        return this;
    }

    /**
     * Sets a groupby.
     *
     * @param groupBy
     * @return
     */
    public Query groupBy(final Expression groupBy) {
        this.groupBy = groupBy;
        return this;
    }

    /**
     * Sets a having clause for the groupby.
     *
     * Note that this is ignored if no groupby is set.
     *
     * @param having
     * @return
     */
    public Query having(final Expression having) {
        this.having = having;
        return this;
    }

    /**
     * Set order criterias for fields and expressions.
     *
     * @param sorts sort values.
     * @return the query object.
     */
    public Query orderBy(final Sort... sorts) {
        this.sorts = sorts;
        return this;
    }

    /**
     * Sets the limit on the Query.
     *
     * @param limit the limit for the rows, 0 or more.
     * @return the query object.
     */
    public Query limit(final int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Limit must be larger or equal to zero.");
        }
        this.limit = limit;
        return this;
    }

    /**
     * Sets the offset for the Query.
     *
     * @param offset the offset.
     * @return the query object.
     */
    public Query offset(final int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be larger or equal to zero.");
        }
        this.offset = offset;
        return this;
    }

    public boolean hasFrom() {
        return from != null;
    }

    /**
     * Exports the query to a raw string.
     *
     * @return the raw N1QL string.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        if (selects != null && selects.length > 0) {
            builder.append("SELECT ");
            for (int i = 0; i < selects.length; i++) {
                builder.append(selects[i]);
                if (i < selects.length-1) {
                    builder.append(",");
                }
                builder.append(" ");
            }
        }

        if (from != null) {
            builder.append("FROM " + from).append(" ");
        }

        if (where != null) {
            builder.append("WHERE " + where.toString()).append(" ");
        }

        if (groupBy != null) {
            builder.append("GROUP BY ").append(groupBy.toString()).append(" ");
            if (having != null) {
                builder.append("HAVING ").append(having.toString()).append(" ");
            }
        }
        if (sorts != null) {
            builder.append("ORDER BY ");
            for (int i = 0; i < sorts.length; i++) {
                builder.append(sorts[i].toString());
                if (i < sorts.length) {
                    builder.append(", ");
                }
            }
        }
        if (limit >= 0) {
            builder.append("LIMIT ").append(limit).append(" ");
        }
        if (offset >= 0) {
            builder.append("OFFSET ").append(offset);
        }

        return builder.toString().trim();
    }
}
