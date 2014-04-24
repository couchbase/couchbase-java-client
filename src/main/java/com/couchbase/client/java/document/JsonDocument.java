package com.couchbase.client.java.document;

import com.couchbase.client.java.document.json.JsonObject;

public class JsonDocument extends AbstractDocument<JsonObject> {

  public JsonDocument() {
  }

  public JsonDocument(String id) {
    super(id);
  }

  public JsonDocument(String id, JsonObject content) {
    super(id, content);
  }

  public JsonDocument(String id, JsonObject content, int expiry) {
    super(id, content, expiry);
  }

  public JsonDocument(String id, JsonObject content, long cas) {
    super(id, content, cas);
  }

  public JsonDocument(String id, JsonObject content, long cas, int expiry) {
    super(id, content, cas, expiry);
  }

}
