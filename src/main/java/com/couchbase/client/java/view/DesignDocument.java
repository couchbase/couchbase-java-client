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
package com.couchbase.client.java.view;

import com.couchbase.client.java.document.json.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
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
    private final Map<Option, Long> options;

    protected DesignDocument(final String name, final List<View> views, Map<Option, Long> options) {
        this.name = name;
        this.views = views;
        this.options = options;
    }

    /**
     * Creates a new {@link DesignDocument}.
     *
     * @param name the name of the design document.
     * @param views all views it contains.
     * @return a new {@link DesignDocument}.
     */
    public static DesignDocument create(final String name, final List<View> views) {
        return create(name, views, new HashMap<Option, Long>());
    }

    /**
     * Creates a new {@link DesignDocument}.
     *
     * @param name the name of the design document.
     * @param views all views it contains.
     * @param options optional options of the design document.
     * @return a new {@link DesignDocument}.
     */
    public static DesignDocument create(final String name, final List<View> views, Map<Option, Long> options) {
        return new DesignDocument(name, views, options);
    }

    /**
     * Create a design document from a JSON representation of it. The JSON is expected to contain 3 JSON objects:
     * a "views" object with an entry for each raw view, a "spatial" object for spatial views and an "options" object
     * for design document options (see {@link Option}).
     *
     * @param name the name of the design document.
     * @param raw the raw JSON representing the design document.
     * @return the corresponding DesignDocument object.
     */
    public static DesignDocument from(final String name, final JsonObject raw) {
        final List<View> views = new ArrayList<View>();

        JsonObject rawViews = raw.getObject("views");
        if (rawViews != null) {
            for(String viewName: rawViews.getNames()) {
                JsonObject viewContent = rawViews.getObject(viewName);
                String map = viewContent.getString("map");
                String reduce = viewContent.getString("reduce");
                views.add(DefaultView.create(viewName, map, reduce));
            }
        }

        JsonObject spatialViews = raw.getObject("spatial");
        if (spatialViews != null) {
            for(String viewName : spatialViews.getNames()) {
                String map = spatialViews.getString(viewName);
                views.add(SpatialView.create(viewName, map));
            }
        }

        JsonObject opts = raw.getObject("options");
        final Map<Option, Long> options = new HashMap<Option, Long>();
        if (opts != null) {
            for (String key : opts.getNames()) {
                options.put(Option.fromName(key), opts.getLong(key));
            }
        }

        return new DesignDocument(name, views, options);
    }

    /**
     * @return the name of the DesignDocument.
     */
    public String name() {
        return name;
    }

    /**
     * Returns a list of the views (raw and spatial) contained in the design document.
     *
     * When you obtain this DesignDocument from the SDK, you can mutate the list. Once you upsert the DesignDocument
     * again, this allows you to add a view or even replace an existing view definition (make sure the updated
     * definition uses the same name as the original and is inserted last).
     *
     * @return the list of {@link View Views} in the design document.
     *
     */
    public List<View> views() {
        return views;
    }

    /**
     * @return the {@link Option Options} set on the design document and their values.
     */
    public Map<Option, Long> options() {
        return options;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DesignDocument{");
        sb.append("name='").append(name).append('\'');
        sb.append(", views=").append(views);
        sb.append(", options=").append(options);
        sb.append('}');
        return sb.toString();
    }

    public JsonObject toJsonObject() {
        JsonObject converted = JsonObject.empty();
        JsonObject views = JsonObject.empty();
        JsonObject spatialViews = JsonObject.empty();
        JsonObject opts = JsonObject.empty();

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

        boolean hasOptions = false;
        for (Map.Entry<Option, Long> entry : options.entrySet()) {
            hasOptions = true;
            opts.put(entry.getKey().alias(), entry.getValue());
        }

        converted.put("views", views);
        converted.put("spatial", spatialViews);
        if (hasOptions) {
            converted.put("options", opts);
        }
        return converted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DesignDocument that = (DesignDocument) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (views != null ? !views.equals(that.views) : that.views != null) return false;
        return !(options != null ? !options.equals(that.options) : that.options != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (views != null ? views.hashCode() : 0);
        result = 31 * result + (options != null ? options.hashCode() : 0);
        return result;
    }

    /**
     * Optional design document options.
     */
    public enum Option {

        /**
         * The minimum changes to perform on a design document before indexing is triggered.
         */
        UPDATE_MIN_CHANGES("updateMinChanges"),

        /**
         * The minimum changes to perform on a design document before replica indexing is triggered.
         */
        REPLICA_UPDATE_MIN_CHANGES("replicaUpdateMinChanges");

        private final String alias;

        Option(String alias) {
            this.alias = alias;
        }

        public String alias() {
            return alias;
        }

        public static Option fromName(final String name) {
            if (name.equals(UPDATE_MIN_CHANGES.alias())) {
                return UPDATE_MIN_CHANGES;
            } else if (name.equals(REPLICA_UPDATE_MIN_CHANGES.alias())) {
                return REPLICA_UPDATE_MIN_CHANGES;
            } else {
                throw new IllegalArgumentException("Unknown name: " + name);
            }
        }
    }
}
