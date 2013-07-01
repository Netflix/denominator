package denominator.hook;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closer;

/**
 * Utilities used for accessing metadata when running on a EC2 instance or
 * otherwise that can access {@code http://169.254.169.254/latest/meta-data/}.
 * 
 * See <a
 *      href="http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AESDG-chapter-instancedata.html">
 *      documentation</a>
 */
public class InstanceMetadataHook {

    /**
     * location of the metadata service
     */
    public static final URI DEFAULT_URI = URI.create("http://169.254.169.254/latest/meta-data/");

    /**
     * Retrieves a list of resources at
     * {@code http://169.254.169.254/latest/meta-data/PATH}, if present.
     * 
     * @param path
     *            path of a listable resource, such as
     *            {@code iam/security-credentials/}; must end in slash
     * @return empty if {@code metadataService} service cannot be contacted or
     *         no data at path.
     */
    public static ImmutableList<String> list(String path) {
        return list(DEFAULT_URI, path);
    }

    /**
     * Retrieves a list of resources at a path, if present.
     * 
     * @param metadataService
     *            endpoint with trailing slash. ex.
     *            {@code http://169.254.169.254/latest/meta-data/}
     * @param path
     *            path of a listable resource, such as
     *            {@code iam/security-credentials/}; must end in slash
     * @return empty if {@code metadataService} service cannot be contacted or
     *         no data at path.
     */
    public static ImmutableList<String> list(URI metadataService, String path) {
        checkArgument(checkNotNull(path, "path").endsWith("/"), "path must end with '/'; %s provided", path);
        Optional<String> content = get(metadataService, path);
        if (content.isPresent())
            return ImmutableList.copyOf(Splitter.on('\n').split(content.get()));
        return ImmutableList.<String> of();
    }

    /**
     * Retrieves a resources at
     * {@code http://169.254.169.254/latest/meta-data/PATH}, if present.
     * 
     * @param path
     *            path to the metadata desired. ex. {@code public-ipv4} or
     *            {@code iam/security-credentials/role-name}
     * @return empty if {@code metadataService} service cannot be contacted or
     *         no data at path.
     */
    public static Optional<String> get(String path) {
        return get(DEFAULT_URI, path);
    }

    /**
     * Retrieves content at a path, if present.
     * 
     * @param metadataService
     *            endpoint with trailing slash. ex.
     *            {@code http://169.254.169.254/latest/meta-data/}
     * @param path
     *            path to the metadata desired. ex. {@code public-ipv4} or
     *            {@code iam/security-credentials/role-name}
     * @return empty if {@code metadataService} service cannot be contacted or
     *         no data at path.
     */
    public static Optional<String> get(URI metadataService, String path) {
        checkNotNull(metadataService, "metadataService");
        checkArgument(metadataService.getPath().endsWith("/"), "metadataService must end with '/'; %s provided",
                metadataService);
        checkNotNull(path, "path");
        Closer closer = Closer.create();
        try {
            InputStream stream = closer.register(openStream(metadataService + path));
            byte[] bytes = ByteStreams.toByteArray(stream);
            if (bytes == null || bytes.length == 0)
                return Optional.<String> absent();
            return Optional.of(new String(bytes));
        } catch (IOException e) {
            return Optional.<String> absent();
        } finally {
            try {
                closer.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * quick http client that allows no-dependency try at getting instance data.
     */
    private static InputStream openStream(String resource) throws IOException {
        HttpURLConnection connection = HttpURLConnection.class.cast(URI.create(resource).toURL().openConnection());
        connection.setConnectTimeout(1000 * 2);
        connection.setReadTimeout(1000 * 2);
        connection.setAllowUserInteraction(false);
        connection.setInstanceFollowRedirects(false);
        return connection.getInputStream();
    }
}