package com.couchbase.client.java.query;

/**
 * Fluent API for a View Query.
 */
public class ViewQuery {

  private final String design;
  private final String view;

  private int limit;
  private boolean development;
  private boolean withDocs;
  private Stale stale = Stale.UPDATE_AFTER;

  private ViewQuery(String design, String view) {
    this.design = design;
    this.view = view;
  }

  public static ViewQuery from(String design, String view) {
    return new ViewQuery(design, view);
  }

  public ViewQuery limit(int limit) {
    this.limit = limit;
    return this;
  }

  public ViewQuery development(boolean development) {
    this.development = development;
    return this;
  }

  public ViewQuery withDocuments(boolean withDocs) {
    this.withDocs = withDocs;
    return this;
  }

  public ViewQuery stale(Stale stale) {
    this.stale = stale;
    return this;
  }

  /**
   * Generates a URI path out of the values.
   *
   * @return the URI path.
   */
  public String query() {
    StringBuilder sb = new StringBuilder();
    sb.append("?stale=").append(stale.identifier());
    return sb.toString();
  }

  public String design() {
    return design;
  }

  public String view() {
    return view;
  }

  public boolean development() {
    return development;
  }

}
