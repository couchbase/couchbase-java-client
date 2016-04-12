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
 * Implementation of a regular, non spatial view.
 *
 * @author Michael Nitschinger
 * @since 2.0.0
 */
public class DefaultView implements View {

    private final String name;
    private final String map;
    private final String reduce;

    protected DefaultView(String name, String map, String reduce) {
        this.name = name;
        this.map = map;
        this.reduce = reduce;
    }

    /**
     * Create a new representation of a regular, non-spatial view.
     *
     * @param name the name of the view.
     * @param map the map function (javascript) for the view.
     * @param reduce the reduce function (prebuilt name or custom javascript) for the view.
     *               Use {@link #create(String, String)} or null if you don't need one.
     * @return the view object.
     */
    public static View create(String name, String map, String reduce) {
        return new DefaultView(name, map, reduce);
    }

    /**
     * Create a new representation of a regular, non-spatial view without reduce function.
     *
     * @param name the name of the view.
     * @param map the map function (javascript) for the view.
     * @return the view object.
     */
    public static View create(String name, String map) {
        return new DefaultView(name, map, null);
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
        return reduce;
    }

    @Override
    public boolean hasReduce() {
        return reduce != null && !reduce.isEmpty();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DefaultView{");
        sb.append("name='").append(name).append('\'');
        sb.append(", map='").append(map).append('\'');
        sb.append(", reduce='").append(reduce).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DefaultView that = (DefaultView) o;

        if (map != null ? !map.equals(that.map) : that.map != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (reduce != null ? !reduce.equals(that.reduce) : that.reduce != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (map != null ? map.hashCode() : 0);
        result = 31 * result + (reduce != null ? reduce.hashCode() : 0);
        return result;
    }
}
