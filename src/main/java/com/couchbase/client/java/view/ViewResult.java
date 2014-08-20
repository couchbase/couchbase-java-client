package com.couchbase.client.java.view;

import com.couchbase.client.java.document.json.JsonObject;
import rx.Observable;

public interface ViewResult {

    Observable<ViewRow> rows();

    int totalRows();

    boolean success();

    JsonObject error();

    JsonObject debug();
}
