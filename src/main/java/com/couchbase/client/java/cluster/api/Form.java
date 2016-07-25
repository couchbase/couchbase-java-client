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
package com.couchbase.client.java.cluster.api;

import java.util.LinkedHashMap;
import java.util.Map;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.deps.io.netty.handler.codec.http.QueryStringEncoder;

/**
 * A utility method / builder class to create form bodies for a {@link RestBuilder} or {@link AsyncRestBuilder}.
 *
 * @author Simon Baslé
 * @since 2.3.2
 */
@InterfaceAudience.Public
@InterfaceStability.Experimental
public class Form {

    private final Map<String, String> formValues;

    private Form() {
        this.formValues = new LinkedHashMap<String, String>();
    }

    /**
     * Create an empty {@link Form}.
     */
    public static Form create() {
        return new Form();
    }

    /**
     * Add a parameter entry to the {@link Form}.
     */
    public Form add(String paramName, String paramValue) {
        this.formValues.put(paramName, paramValue);
        return this;
    }

    /**
     * Encode the {@link Form} using the "application/x-www-form-urlencoded" Content-Type.
     * Each form parameter is represented as "key=value" where both key and value are url-encoded.
     *
     * @return the url-encoded representation of the form.
     */
    public String toUrlEncodedString() {
        QueryStringEncoder encoder = new QueryStringEncoder("");
        for (Map.Entry<String, String> entry : formValues.entrySet()) {
            encoder.addParam(entry.getKey(), entry.getValue());
        }
        StringBuilder ues = new StringBuilder(encoder.toString());
        if (ues.length() > 0 && ues.charAt(0) == '?') {
            ues.deleteCharAt(0);
        }
        return ues.toString();
    }

}
