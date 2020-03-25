/*
 * Copyright (c) 2017 Couchbase, Inc.
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
package com.couchbase.client.java.search.queries;

import java.util.List;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;

/**
 * A FTS query which allows to match on geo polygons.
 *
 * @author Jyotsna Nayak
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class GeoPolygonQuery extends AbstractFtsQuery {

    private final List<Coordinate> coordinates;

    private String field;

    public GeoPolygonQuery(final List<Coordinate> coordinates) {
        this.coordinates = coordinates;
    }

    public GeoPolygonQuery field(final String field) {
        this.field = field;
        return this;
    }

    @Override
    public GeoPolygonQuery boost(double boost) {
        super.boost(boost);
        return this;
    }

    @Override
    protected void injectParams(final JsonObject input) {
        JsonArray points = JsonArray.empty();
        for (Coordinate coordinate : coordinates) {
            points.add(JsonArray.from(coordinate.lon(), coordinate.lat()));
        }
        input.put("polygon_points", points);

        if (field != null) {
            input.put("field", field);
        }
    }
}
