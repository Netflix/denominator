package denominator.hook;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.slurp;
import static denominator.common.Util.split;

/**
 * Utilities used for accessing metadata when running on a EC2 instance or otherwise that can access
 * {@code http://169.254.169.254/latest/meta-data/}.
 *
 * See <a href= "http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AESDG-chapter-instancedata.html"
 * > documentation</a>
 */
public class InstanceMetadataHook {

  /**
   * location of the metadata service
   */
  public static final URI DEFAULT_URI = URI.create("http://169.254.169.254/latest/meta-data/");

  /**
   * Retrieves a list of resources at {@code http://169.254.169.254/latest/meta-data/PATH}, if
   * present.
   *
   * @param path path of a listable resource, such as {@code iam/security-credentials/}; must end in
   *             slash
   * @return empty if {@code metadataService} service cannot be contacted or no data at path.
   */
  public static List<String> list(String path) {
    return list(DEFAULT_URI, path);
  }

  /**
   * Retrieves a list of resources at a path, if present.
   *
   * @param metadataService endpoint with trailing slash. ex. {@code http://169.254.169.254/latest/meta-data/}
   * @param path            path of a listable resource, such as {@code iam/security-credentials/};
   *                        must end in slash
   * @return empty if {@code metadataService} service cannot be contacted or no data at path.
   */
  public static List<String> list(URI metadataService, String path) {
    checkArgument(checkNotNull(path, "path").endsWith("/"), "path must end with '/'; %s provided",
                  path);
    String content = get(metadataService, path);
    if (content != null) {
      return split('\n', content);
    }
    return Collections.<String>emptyList();
  }

  /**
   * Retrieves a resources at {@code http://169.254.169.254/latest/meta-data/PATH}, if present.
   *
   * @param path path to the metadata desired. ex. {@code public-ipv4} or {@code
   *             iam/security-credentials/role-name}
   * @return null if {@code metadataService} service cannot be contacted or no data at path.
   */
  public static String get(String path) {
    return get(DEFAULT_URI, path);
  }

  /**
   * Retrieves content at a path, if present.
   *
   * @param metadataService endpoint with trailing slash. ex. {@code http://169.254.169.254/latest/meta-data/}
   * @param path            path to the metadata desired. ex. {@code public-ipv4} or {@code
   *                        iam/security-credentials/role-name}
   * @return null if {@code metadataService} service cannot be contacted or no data at path.
   */
  public static String get(URI metadataService, String path) {
    checkNotNull(metadataService, "metadataService");
    checkArgument(metadataService.getPath().endsWith("/"),
                  "metadataService must end with '/'; %s provided",
                  metadataService);
    checkNotNull(path, "path");
    InputStream stream = null;
    try {
      stream = openStream(metadataService + path);
      String content = slurp(new InputStreamReader(stream));
      if (content.isEmpty()) {
        return null;
      }
      return content;
    } catch (IOException e) {
      return null;
    } finally {
      try {
        if (stream != null) {
          stream.close();
        }
      } catch (IOException e) {
      }
    }
  }

  /**
   * quick http client that allows no-dependency try at getting instance data.
   */
  private static InputStream openStream(String resource) throws IOException {
    HttpURLConnection
        connection =
        HttpURLConnection.class.cast(URI.create(resource).toURL().openConnection());
    connection.setConnectTimeout(1000 * 2);
    connection.setReadTimeout(1000 * 2);
    connection.setAllowUserInteraction(false);
    connection.setInstanceFollowRedirects(false);
    return connection.getInputStream();
  }
}
