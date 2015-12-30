package com.couchbase.client.java.query.dsl.element;

import com.couchbase.client.java.query.dsl.Expression;

public class MergeInsertElement implements Element {

  private final Expression expression;

  public MergeInsertElement(Expression expression) {
    if (expression == null) {
      throw new IllegalArgumentException("Expression on Merge Insert is required.");
    }

    this.expression = expression;
  }

  @Override
  public String export() {
    return "WHEN NOT MATCHED THEN INSERT " + expression.toString();
  }
}
