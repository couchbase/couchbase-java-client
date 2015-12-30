package com.couchbase.client.java.query.dsl.element;

public class MergeDeleteElement implements Element {

  @Override
  public String export() {
    return "WHEN MATCHED THEN DELETE";
  }
}
