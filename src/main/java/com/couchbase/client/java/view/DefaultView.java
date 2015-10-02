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
