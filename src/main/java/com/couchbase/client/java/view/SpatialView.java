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

/**
 * Implementation of a spatial view.
 *
 * @author Michael Nitschinger
 * @since 2.1.0
 */
public class SpatialView implements View {

    private final String name;
    private final String map;

    protected SpatialView(String name, String map) {
        this.name = name;
        this.map = map;
    }

    /**
     * Create a new representation of a spatial view.
     *
     * @param name the name of the spatial view.
     * @param map the map function (javascript) for the spatial view.
     * @return the spatial view object.
     */
    public static View create(String name, String map) {
        return new SpatialView(name, map);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String map() {
        return map;
    }

    @Override
    public String reduce() {
        return null;
    }

    @Override
    public boolean hasReduce() {
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SpatialView{");
        sb.append("name='").append(name).append('\'');
        sb.append(", map='").append(map).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpatialView that = (SpatialView) o;

        if (map != null ? !map.equals(that.map) : that.map != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (map != null ? map.hashCode() : 0);
        return result;
    }
}
