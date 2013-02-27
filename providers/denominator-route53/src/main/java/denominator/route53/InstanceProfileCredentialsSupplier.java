package denominator.route53;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.jclouds.util.Strings2.toStringAndClose;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import denominator.Credentials;
import denominator.Credentials.MapCredentials;

/**
 * Credentials supplier implementation that loads credentials from the Amazon
 * EC2 Instance Metadata Service.
 */
public class InstanceProfileCredentialsSupplier implements Supplier<Credentials> {
    private final Supplier<String> iipJsonSupplier;

    public InstanceProfileCredentialsSupplier() {
        this(new ReadFirstInstanceProfileCredentialsOrNull());
    }

    public InstanceProfileCredentialsSupplier(String baseUri) {
        this(new ReadFirstInstanceProfileCredentialsOrNull(baseUri));
    }

    public InstanceProfileCredentialsSupplier(Supplier<String> iipJsonSupplier) {
        this.iipJsonSupplier = checkNotNull(iipJsonSupplier, "iipJsonSupplier");
    }

    @Override
    public Credentials get() {
        return MapCredentials.from(parseJson(iipJsonSupplier.get()));
    }

    private static final Map<String, String> keyMap = ImmutableMap.of("AccessKeyId", "accessKey", "SecretAccessKey",
            "secretKey", "Token", "sessionToken");

    /**
     * IAM Instance Profile format is simple, non-nested json.
     * 
     * ex.
     * 
     * <pre>
     * {
     *   "Code" : "Success",
     *   "LastUpdated" : "2013-02-26T02:03:57Z",
     *   "Type" : "AWS-HMAC",
     *   "AccessKeyId" : "AAAAA",
     *   "SecretAccessKey" : "SSSSSSS",
     *   "Token" : "TTTTTTT",
     *   "Expiration" : "2013-02-26T08:12:23Z"
     * }
     * 
     * </pre>
     * 
     * This impl avoids choosing a json library by parsing the simple structure
     * above directly.
     * 
     */
    static Map<String, String> parseJson(String in) {
        if (in == null)
            return ImmutableMap.of();
        String noBraces = in.replace('{', ' ').replace('}', ' ').trim();
        Builder<String, String> builder = ImmutableMap.<String, String> builder();
        for (Entry<String, String> entry : Splitter.on(',').withKeyValueSeparator(" : ").split(noBraces).entrySet()) {
            String key = keyMap.get(entry.getKey().replace('"', ' ').trim());
            if (key != null)
                builder.put(key, entry.getValue().replace('"', ' ').trim());
        }
        return builder.build();
    }

    /**
     * default means to grab instance credentials, or return null
     */
    static class ReadFirstInstanceProfileCredentialsOrNull implements Supplier<String> {
        private final String baseUri;

        public ReadFirstInstanceProfileCredentialsOrNull() {
            this("http://169.254.169.254/");
        }

        /**
         * @param baseUri
         *            uri string with trailing slash
         */
        public ReadFirstInstanceProfileCredentialsOrNull(String baseUri) {
            this.baseUri = checkNotNull(baseUri, "baseUri");
        }

        @Override
        public String get() {
            String resource = baseUri + "latest/meta-data/iam/security-credentials/";
            try {
                String output = toStringAndClose(openStream(resource)).trim();
                List<String> roles = ImmutableList.copyOf(Splitter.on('\n').split(output));
                if (roles.isEmpty())
                    return null;
                return toStringAndClose(openStream(resource + roles.get(0)));
            } catch (IOException e) {
                return null;
            }
        }

        private InputStream openStream(String resource) throws IOException {
            HttpURLConnection connection = HttpURLConnection.class.cast(URI.create(resource).toURL().openConnection());
            connection.setConnectTimeout(1000 * 2);
            connection.setReadTimeout(1000 * 2);
            connection.setAllowUserInteraction(false);
            connection.setInstanceFollowRedirects(false);
            return connection.getInputStream();
        }

        @Override
        public String toString() {
            return "ReadFirstInstanceProfileCredentialsOrNull(" + baseUri + ")";
        }
    }
}