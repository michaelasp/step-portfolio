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

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ImagesServiceFailureException;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    String imageUrl;

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
      comment.imageUrl = (String) entity.getProperty("imageUrl");
      comments.add(comment);
    }
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Gson gson = new Gson();

    // Creates a comment for every post request
    Comment comment = new Comment();
    comment.name = getParameter(request, "commenter");
    comment.text = getParameter(request, "comment");
    comment.timestamp = System.currentTimeMillis();
    comment.imageUrl = getFileUrl(request, "image");

    comments.add(comment);
    addCommentData(comment);
    response.sendRedirect("/comments.html");
  }

  private void addCommentData(Comment comment) {
    Entity commentEntity = new Entity("comment");
    commentEntity.setProperty("name", comment.name);
    commentEntity.setProperty("text", comment.text);
    commentEntity.setProperty("timestamp", comment.timestamp);
    commentEntity.setProperty("imageUrl", comment.imageUrl);

    DatastoreService dataStore = DatastoreServiceFactory.getDatastoreService();
    dataStore.put(commentEntity);
  }

  private String getParameter(HttpServletRequest request, String name) {
    String value = request.getParameter(name);
    if (value == null) {
      return "";
    }
    return value;
  }

  /**
   * Returns a URL that points to the uploaded file, or default profile if the user didn't upload a
   * file.
   */
  private String getFileUrl(HttpServletRequest request, String formInputElementName) {
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(request);
    List<BlobKey> blobKeys = blobs.get("image");

    // User submitted form without selecting a file, so we can't get a URL. (dev server)
    if (blobKeys == null || blobKeys.isEmpty()) {
      return "/images/default.png";
    }

    // Our form only contains a single file input, so get the first index.
    BlobKey blobKey = blobKeys.get(0);

    // User submitted form without selecting a file, so we can't get a URL. (live server)
    BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(blobKey);
    if (blobInfo.getSize() == 0) {
      blobstoreService.delete(blobKey);
      return "/images/default.png";
    }

    // We could check the validity of the file here, e.g. to make sure it's an image file
    // https://stackoverflow.com/q/10779564/873165

    // Use ImagesService to get a URL that points to the uploaded file.
    ImagesService imagesService = ImagesServiceFactory.getImagesService();
    ServingUrlOptions options = ServingUrlOptions.Builder.withBlobKey(blobKey);

    // To support running in Google Cloud Shell with AppEngine's devserver, we must use the relative
    // path to the image, rather than the path returned by imagesService which contains a host.
    try {
      URL url = new URL(imagesService.getServingUrl(options));
      return url.getPath();
    } catch (MalformedURLException e) {
      return imagesService.getServingUrl(options);
    } catch (ImagesServiceFailureException e) {
      throw e;
    }
  }
}
