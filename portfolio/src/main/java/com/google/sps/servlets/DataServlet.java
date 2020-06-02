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

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;

/** Servlet that returns some example content. TODO: modify this file to handle comments data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {
  private class Comment {
      String name;
      String text;
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
    if (!loaded) {
        loadCommentData(request);
    }
    int requestAmount;
    try {
        requestAmount = Integer.parseInt(request.getParameter("amount"));
    } catch (NumberFormatException e) {
        requestAmount = 0;
    }
    if(requestAmount < 0) {
        requestAmount = 0;
    }
    //Reduce request size to be the comments size to not get exception
    if(requestAmount > comments.size()) {
        requestAmount = comments.size();
    }
    String json = arrayToJSON(comments.subList(0, requestAmount));
    response.setContentType("text/html;");
    response.getWriter().println(json);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Gson gson = new Gson();

    //Creates a comment for every post request
    Comment comment = new Comment();
    comment.name = getParameter(request, "commenter");
    comment.text = getParameter(request, "comment");
    
    comments.add(comment);
    addCommentData(comment);

    response.setContentType("text/html;");
    response.getWriter().println("Done");
  }

  private void addCommentData(Comment comment) {
      Entity commentEntity = new Entity("comment");
      commentEntity.setProperty("name", comment.name);
      commentEntity.setProperty("text", comment.text);

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
