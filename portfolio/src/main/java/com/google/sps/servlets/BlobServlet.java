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

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/get-blob")
public class BlobServlet extends HttpServlet {
  private BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String key = request.getParameter("blobKey");
    System.out.println(Paths.get(".").toAbsolutePath().normalize().toString());
    if (key.equals("")) {
      response.setContentType("image/png");
      File file = new File("./images/default.png");
      response.setContentLength((int) file.length());
      FileInputStream in = new FileInputStream(file);
      OutputStream out = response.getOutputStream();

      // Copy the contents of the file to the output stream
      byte[] buf = new byte[1024];
      int count = 0;
      while ((count = in.read(buf)) >= 0) {
        out.write(buf, 0, count);
      }
      out.close();
      in.close();
    } else {
      BlobKey blobKey = new BlobKey(key);
      blobstoreService.serve(blobKey, response);
    }
  }
}
