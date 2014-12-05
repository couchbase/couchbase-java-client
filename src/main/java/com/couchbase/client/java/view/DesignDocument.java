/**
 * Copyright (C) 2014 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */
package com.couchbase.client.java.view;

import com.couchbase.client.java.document.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a design document to store and load.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class DesignDocument {

    private final String name;
    private final List<View> views;

    protected DesignDocument(final String name, final List<View> views) {
        this.name = name;
        this.views = views;
    }

    /**
     * Creates a new {@link DesignDocument}.
     *
     * @param name the name of the design document.
     * @param views all views it contains.
     * @return a new {@link DesignDocument}.
     */
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
        JsonObject spatialViews = raw.getObject("spatial");
        if (spatialViews != null) {
            for(Map.Entry<String, Object> entry : spatialViews.toMap().entrySet()) {
                String viewName = entry.getKey();
                String map = (String) entry.getValue();
                views.add(SpatialView.create(viewName, map));
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
        JsonObject spatialViews = JsonObject.empty();

        for (View view : this.views) {
            if (view instanceof SpatialView) {
                spatialViews.put(view.name(), view.map());
            } else {
                JsonObject content = JsonObject.empty();
                content.put("map", view.map());
                if (view.hasReduce()) {
                    content.put("reduce", view.reduce());
                }
                views.put(view.name(), content);
            }
        }

        converted.put("views", views);
        converted.put("spatial", spatialViews);
        return converted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DesignDocument that = (DesignDocument) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (views != null ? !views.equals(that.views) : that.views != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (views != null ? views.hashCode() : 0);
        return result;
    }
}
