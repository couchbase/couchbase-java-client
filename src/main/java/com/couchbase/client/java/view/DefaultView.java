package com.couchbase.client.java.view;

/**
 * .
 *
 * @author Michael Nitschinger
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
    public static View create(String name, String map, String reduce) {
        return new DefaultView(name, map, reduce);
    }

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
