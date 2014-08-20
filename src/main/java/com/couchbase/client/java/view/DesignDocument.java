package com.couchbase.client.java.view;

import com.couchbase.client.java.document.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DesignDocument {

    private final String name;
    private final List<View> views;

    protected DesignDocument(final String name, final List<View> views) {
        this.name = name;
        this.views = views;
    }

    public static DesignDocument create(final String name, final List<View> views) {
        return new DesignDocument(name, views);
    }

    public static DesignDocument from(final String name, final JsonObject raw) {
        final List<View> views = new ArrayList<View>();
        JsonObject rawViews = raw.getObject("views");
        if (rawViews != null) {
           for(Map.Entry<String, Object> entry : rawViews.toMap().entrySet()) {
               String viewName = entry.getKey();
               JsonObject viewContent = (JsonObject) entry.getValue();
               String map = viewContent.getString("map");
               String reduce = viewContent.getString("reduce");
               views.add(DefaultView.create(viewName, map, reduce));
            }
        }
        return new DesignDocument(name, views);
    }

    public String name() {
        return name;
    }

    public List<View> views() {
        return views;
    }

    @Override
    public String toString() {
        return "DesignDocument{" + "name='" + name + '\'' + ", views=" + views + '}';
    }


    public JsonObject toJsonObject() {
        JsonObject converted = JsonObject.empty();
        JsonObject views = JsonObject.empty();

        for (View view : this.views) {
            JsonObject content = JsonObject.empty();
            content.put("map", view.map());
            if (view.hasReduce()) {
                content.put("reduce", view.reduce());
            }
            views.put(view.name(), content);
        }

        converted.put("views", views);
        return converted;
    }
}
