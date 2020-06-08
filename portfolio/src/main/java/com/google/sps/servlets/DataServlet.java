// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that returns some example content. TODO: modify this file to handle comments data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {

  private static final int DEFAULT_COMMENTS = 2;

  private class Comment implements Comparable<Comment> {
    String name;
    String text;
    long timestamp;

    @Override
    public int compareTo(Comment otherComment) {
      // Compares in descending order in order to get newest comments first
      if (this.timestamp >= otherComment.timestamp) {
        return -1;
      } else {
        return 1;
      }
    }
  }

  private List<String> facts;
  private List<Comment> comments;
  boolean loaded = false;

  @Override
  public void init() {
    comments = new ArrayList<>();
  }

  private String arrayToJSON(List<Comment> strList) {
    Gson gson = new Gson();

    String json = gson.toJson(strList);
    return json;
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    loadCommentData(request);

    int requestAmount;
    int pageNumber;
    try {
      requestAmount = Integer.parseInt(request.getParameter("amount"));
    } catch (NumberFormatException e) {
      requestAmount = DEFAULT_COMMENTS;
    }

    try {
      pageNumber = Integer.parseInt(request.getParameter("page"));
    } catch (NumberFormatException e) {
      pageNumber = 0;
    }
    if (requestAmount < 0) {
      requestAmount = DEFAULT_COMMENTS;
    }
    // Reduce request size to be the comments size to not get exception
    if (requestAmount > comments.size()) {
      requestAmount = comments.size();
    }

    int offsetAmount = pageNumber * requestAmount;
    if (offsetAmount < 0) {
      offsetAmount = 0;
    }
    if (offsetAmount > comments.size()) {
      offsetAmount = comments.size();
    }

    if (requestAmount + offsetAmount > comments.size()) {
      requestAmount = comments.size();
    } else {
      requestAmount += offsetAmount;
    }

    Collections.sort(comments);
    String json = arrayToJSON(comments.subList(offsetAmount, requestAmount));
    response.setContentType("text/html;");
    response.getWriter().println(json);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Gson gson = new Gson();

    // Creates a comment for every post request

    Comment comment = new Comment();
    comment.name = getParameter(request, "commenter");
    comment.text = getParameter(request, "comment");
    comment.timestamp = System.currentTimeMillis();

    comments.add(comment);
    addCommentData(comment);
    response.sendRedirect("/comments.html");
  }

  private void addCommentData(Comment comment) {
    Entity commentEntity = new Entity("comment");
    commentEntity.setProperty("name", comment.name);
    commentEntity.setProperty("text", comment.text);
    commentEntity.setProperty("timestamp", comment.timestamp);
    DatastoreService dataStore = DatastoreServiceFactory.getDatastoreService();
    dataStore.put(commentEntity);
  }

  private void loadCommentData(HttpServletRequest request) {
    comments.clear();
    DatastoreService dataStore = DatastoreServiceFactory.getDatastoreService();

    Query query = new Query("comment");
    PreparedQuery results = dataStore.prepare(query);

    for (Entity entity : results.asIterable()) {
      Comment comment = new Comment();
      comment.name = (String) entity.getProperty("name");
      comment.text = (String) entity.getProperty("text");
      comment.timestamp = (long) entity.getProperty("timestamp");
      comments.add(comment);
    }
  }

  private String getParameter(HttpServletRequest request, String name) {
    String value = request.getParameter(name);
    if (value == null) {
      return "";
    }
    return value;
  }
}
