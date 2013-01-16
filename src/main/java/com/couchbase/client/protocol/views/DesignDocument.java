/**
 * Copyright (C) 2009-2012 Couchbase, Inc.
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

package com.couchbase.client.protocol.views;

import java.util.ArrayList;
import java.util.List;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * The DesignDocument represents a design document stored and retrieved from a
 * Couchbase cluster.
 */
public class DesignDocument<T> {

  /**
   * The name of the design document.
   */
  private String name;

  /**
   * The language of the views.
   *
   * Only "javascript" is supported currently.
   */
  private String language = "javascript";

  /**
   * Associated views to the design document.
   */
  private List<ViewDesign> views;

  /**
   * Associated spatial views to the design document.
   */
  private List<SpatialViewDesign> spatialViews;

  /**
   * Create a new DesignDocument with a name.
   *
   * If this constructor is used, the various set methods for the views
   * need to be used in order to provide at least one view. A design document
   * without a view is not allowed and will return an exception when casted
   * into a JSON string.
   *
   * @param name the name of the DesignDocument.
   */
  public DesignDocument(String name) {
    this(name, new ArrayList<ViewDesign>(), new ArrayList<SpatialViewDesign>());
  }

  /**
   * Create a new DesignDocument with a name and (spatial) views.
   *
   * It is possible to pass null for either views or spatialViews. In this case,
   * no views of that type will be created.
   *
   * @param name the name of the DesignDocument.
   * @param views a list of ViewDesigns, which represent the views.
   * @param spatialViews a list of SpatialViewDesigns, which represent
   *    the spatial views.
   */
  public DesignDocument(String name, List<ViewDesign> views,
    List<SpatialViewDesign> spatialViews) {

    if(views == null) {
      views = new ArrayList<ViewDesign>();
    }
    if(spatialViews == null) {
      spatialViews = new ArrayList<SpatialViewDesign>();
    }

    this.name = name;
    this.views = views;
    this.spatialViews = spatialViews;
  }

  /**
   * Get the language of the views.
   *
   * @return the language of the views.
   */
  public String getLanguage() {
    return language;
  }

  /**
   * Get the name of the design document.
   *
   * @return the name of the design document.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the list of the associated view designs.
   *
   * @return a list of the associated view designs.
   */
  public List<ViewDesign> getViews() {
    return views;
  }

  /**
   * Set a list of ViewDesigns.
   *
   * @param v the list of ViewDesign objects representing the views.
   * @return the current object instance.
   */
  public DesignDocument setViews(List<ViewDesign> v) {
    this.views = v;
    return this;
  }

  /**
   * Add a single view to the list of stored ViewDesign objects.
   *
   * @param view a single view to be added.
   * @return the current object instance.
   */
  public DesignDocument setView(ViewDesign view) {
    this.views.add(view);
    return this;
  }

  /**
   * Return a list of all stored spatial views.
   *
   * @return the list of stored spatial views.
   */
  public List<SpatialViewDesign> getSpatialViews() {
    return spatialViews;
  }

  /**
   * Set a list of SpatialViewDesigns.
   *
   * @param sv the list of SpatialViewDesign objects.
   * @return the current object instance.
   */
  public DesignDocument setSpatialViews(List<SpatialViewDesign> sv) {
    this.spatialViews = sv;
    return this;
  }

  /**
   * Add a single spatial view to the list of stored DpatialViewDesign objects.
   *
   * @param spatialView a single spatial view to be added.
   * @return the current object instance.
   */
  public DesignDocument setSpatialView(SpatialViewDesign spatialView) {
    this.spatialViews.add(spatialView);
    return this;
  }

  /**
   * Set the name of the design document.
   *
   * @param n the name of the design document.
   * @return the current object instance.
   */
  public DesignDocument setName(String n) {
    this.name = n;
    return this;
  }

  /**
   * Create a JSON representation of the design document.
   *
   * This JSON representation mirrors the needed format of the Couchbase
   * server implementation.
   *
   * @return the JSON representation of the design document.
   * @throws RuntimeException when no view or no name is set.
   */
  public String toJson() {
    if(views.isEmpty() && spatialViews.isEmpty()) {
      throw new RuntimeException("A design document needs a view");
    }

    if(name.isEmpty()) {
      throw new RuntimeException("A design document needs a name.");
    }

    JSONObject jsonDesign = new JSONObject();
    try {
      jsonDesign.accumulate("language", language);

      JSONObject jsonViews = new JSONObject();
      for(ViewDesign view : views) {
        JSONObject jsonView = new JSONObject();
        jsonView.accumulate("map", view.getMap());
        if(!view.getReduce().isEmpty()) {
          jsonView.accumulate("reduce", view.getReduce());
        }
        jsonViews.accumulate(view.getName(), jsonView);
      }
      jsonDesign.accumulate("views", jsonViews);

      if(!spatialViews.isEmpty()) {
        JSONObject jsonSpatialViews = new JSONObject();
        for(SpatialViewDesign spatialView : spatialViews) {
          jsonSpatialViews.accumulate(spatialView.getName(),
            spatialView.getMap());
        }
        jsonDesign.accumulate("spatial", jsonSpatialViews);
      }
    } catch (JSONException ex) {
      throw new RuntimeException("Failed to compose design document: " + ex);
    }

    return jsonDesign.toString();
  }
}
