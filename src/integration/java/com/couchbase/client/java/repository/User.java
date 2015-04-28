/**
 * Copyright (C) 2015 Couchbase, Inc.
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
package com.couchbase.client.java.repository;

import com.couchbase.client.java.repository.annotation.Field;

public class User {

    private String id;

    @Field
    private String name;

    @Field
    private boolean published;

    @Field
    private String nullField = null;

    @Field("num")
    private int someNumber;

    @Field
    private double otherNumber;

    public User() {

    }

    public User(String name, boolean published, int someNumber, double otherNumber) {
        id = "user::" + name;
        this.name = name;
        this.published = published;
        this.someNumber = someNumber;
        this.otherNumber = otherNumber;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public boolean published() {
        return published;
    }

    public int someNumber() {
        return someNumber;
    }

    public double otherNumber() {
        return otherNumber;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("User{");
        sb.append("id='").append(id).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", published=").append(published);
        sb.append(", someNumber=").append(someNumber);
        sb.append(", otherNumber=").append(otherNumber);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (published != user.published) return false;
        if (someNumber != user.someNumber) return false;
        if (Double.compare(user.otherNumber, otherNumber) != 0) return false;
        if (id != null ? !id.equals(user.id) : user.id != null) return false;
        if (name != null ? !name.equals(user.name) : user.name != null) return false;
        return !(nullField != null ? !nullField.equals(user.nullField) : user.nullField != null);

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (published ? 1 : 0);
        result = 31 * result + (nullField != null ? nullField.hashCode() : 0);
        result = 31 * result + someNumber;
        temp = Double.doubleToLongBits(otherNumber);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
