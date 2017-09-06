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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;

/**
 * A FTS query which allows to match geo bounding boxes.
 *
 * @author Michael Nitschinger
 * @since 2.4.5
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Public
public class GeoBoundingBoxQuery extends AbstractFtsQuery {

    private final double topLeftLat;
    private final double topLeftLon;
    private final double bottomRightLat;
    private final double bottomRightLon;
    private String field;

    public GeoBoundingBoxQuery(double topLeftLon, double topLeftLat, double bottomRightLon, double bottomRightLat) {
        this.topLeftLat = topLeftLat;
        this.topLeftLon = topLeftLon;
        this.bottomRightLat = bottomRightLat;
        this.bottomRightLon = bottomRightLon;
    }

    public GeoBoundingBoxQuery field(String field) {
        this.field = field;
        return this;
    }

    @Override
    public GeoBoundingBoxQuery boost(double boost) {
        super.boost(boost);
        return this;
    }

    @Override
    protected void injectParams(JsonObject input) {
        input.put("top_left", JsonArray.from(topLeftLon, topLeftLat));
        input.put("bottom_right", JsonArray.from(bottomRightLon, bottomRightLat));
        if (field != null) {
            input.put("field", field);
        }
    }
}
